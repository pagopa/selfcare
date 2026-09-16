#!/usr/bin/env python3
"""Backfill the multitenant `tenantId` discriminator on pre-existing documents.

Step_1 EPIC sub-task 10 (apps/docs/Multitenant/Step_1/EPIC.md).

Every tenant-scoped read currently matches `tenantId = <tenant> OR tenantId IS NULL`, because a
strict filter would have hidden every pre-existing document from both tenants the day it shipped.
That `OR` also means an untagged document is readable by *both* tenants, so the isolation is not a
security boundary until this backfill has run everywhere and the branch has been removed from the
services.

The tenant and expected Cosmos account name are explicit arguments. Before connecting, the script
checks that MONGO_HOST names that exact account and that the account naming registered by the
platform belongs to the requested tenant. A wrong connection string therefore fails before any
document can be labelled.

Two invariants make the script safe to re-run and safe to interrupt:
  * only documents WITHOUT a `tenantId` are touched, so a document already attributed to a tenant is
    never reassigned — a replay, or a run with the wrong tenant argument after a correct one, cannot
    move data between tenants;
  * any existing non-null `tenantId` different from the requested tenant aborts the whole run before
    the first write;
  * nothing is written unless --apply is passed.
"""

import argparse
import os
import sys

from pymongo import MongoClient
from pymongo.errors import PyMongoError
from pymongo.uri_parser import parse_uri

# Databases and collections whose entities declare a `tenantId` field, derived from the @MongoEntity
# / @Document annotations in the services that own them. Anything not listed here is deliberately
# out of scope, in particular the product catalogue database used by `product`/`product-cdc`: it
# holds the global, cross-tenant product definitions and tagging it would be wrong, not merely
# unnecessary.
TENANT_SCOPED_COLLECTIONS = {
    "selcAuth": ["otpFlows"],
    "selcDocument": ["documents"],
    "selcIam": ["userClaims"],
    "selcMsCore": ["Institution", "Delegations", "MailNotification"],
    "selcOnboarding": ["onboardings", "tokens"],
    "selcUser": ["userInfo", "userInstitutions"],
    "selcUserGroup": ["UserGroups"],
    "selcWebhook": ["webhooks", "webhookNotifications"],
}

TENANTS = ["AR", "PNPG"]

UNTAGGED = {
    "$or": [
        {"tenantId": {"$exists": False}},
        {"$expr": {"$eq": [{"$type": "$tenantId"}, "null"]}},
    ]
}


class AnsiColors:
    OK = "\033[92m"
    WARNING = "\033[93m"
    ERROR = "\033[91m"
    ENDC = "\033[0m"


def parseArguments():
    parser = argparse.ArgumentParser(
        description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--tenant", required=True, choices=TENANTS,
                        help="Tenant owning the data in the database MONGO_HOST points at.")
    parser.add_argument("--account-name", required=True,
                        help="Expected Cosmos DB account name. It must match both MONGO_HOST and --tenant.")
    parser.add_argument("--database", action="append", choices=sorted(TENANT_SCOPED_COLLECTIONS),
                        help="Restrict to one database; repeatable. Defaults to every listed database.")
    parser.add_argument("--allow-missing", action="append", default=[], metavar="DATABASE.COLLECTION",
                        help="Explicitly allow one expected collection to be absent; repeatable.")
    parser.add_argument("--apply", action="store_true",
                        help="Actually write. Without it the script only reports what it would tag.")
    parser.add_argument("--verify", action="store_true",
                        help="Report how many documents are still untagged and exit. Use this to decide "
                             "whether the 'or tenantId is null' read branch can be dropped.")
    return parser.parse_args()


def selectedDatabases(requested):
    return requested if requested else sorted(TENANT_SCOPED_COLLECTIONS)


def mismatchedTenant(tenant):
    return {
        "tenantId": {"$exists": True},
        "$expr": {
            "$and": [
                {"$ne": [{"$type": "$tenantId"}, "null"]},
                {"$ne": ["$tenantId", tenant]},
            ]
        },
    }


def tenantForAccount(accountName):
    if accountName.endswith("-pnpg-cosmosdb-mongodb-account"):
        return "PNPG"
    if accountName.endswith("-cosmosdb-mongodb-account"):
        return "AR"
    raise ValueError(
        f"Cosmos account {accountName!r} does not match a registered AR or PNPG account name")


def accountNamesFromMongoHost(mongoHost):
    try:
        hosts = parse_uri(mongoHost)["nodelist"]
    except Exception as e:
        raise ValueError(f"Cannot parse MONGO_HOST: {e}") from e

    accountNames = {host.split(".", 1)[0] for host, _ in hosts}
    if len(accountNames) != 1:
        raise ValueError(
            f"MONGO_HOST must resolve to one Cosmos account, found: {', '.join(sorted(accountNames))}")
    return accountNames


def validateAccount(mongoHost, expectedAccountName, tenant):
    accountNames = accountNamesFromMongoHost(mongoHost)
    if accountNames != {expectedAccountName}:
        raise ValueError(
            f"MONGO_HOST points to {next(iter(accountNames))!r}, not expected account "
            f"{expectedAccountName!r}")
    accountTenant = tenantForAccount(expectedAccountName)
    if accountTenant != tenant:
        raise ValueError(
            f"Cosmos account {expectedAccountName!r} belongs to tenant {accountTenant}, "
            f"not requested tenant {tenant}")


def validateAllowedMissing(allowedMissing):
    known = {
        f"{database}.{collection}"
        for database, collections in TENANT_SCOPED_COLLECTIONS.items()
        for collection in collections
    }
    unknown = set(allowedMissing) - known
    if unknown:
        raise ValueError(
            f"Unknown --allow-missing value(s): {', '.join(sorted(unknown))}")
    return set(allowedMissing)


def resolveCollections(client, databases, allowedMissing):
    resolved = []
    for databaseName in databases:
        database = client[databaseName]
        present = set(database.list_collection_names())

        for collectionName in TENANT_SCOPED_COLLECTIONS[databaseName]:
            if collectionName not in present:
                qualifiedName = f"{databaseName}.{collectionName}"
                if qualifiedName not in allowedMissing:
                    raise RuntimeError(
                        f"Expected collection {qualifiedName} is absent. If this is intentional, "
                        f"repeat the command with --allow-missing {qualifiedName}.")
                print(f"  {AnsiColors.WARNING}SKIP{AnsiColors.ENDC} {qualifiedName}: "
                      "explicitly allowed to be absent")
                continue
            resolved.append((databaseName, collectionName, database[collectionName]))

    if not resolved:
        raise RuntimeError("No expected collection was inspected; refusing to report success.")
    return resolved


def scanCollections(collections, tenant):
    scan = []
    for databaseName, collectionName, collection in collections:
        untagged = collection.count_documents(UNTAGGED)
        mismatched = collection.count_documents(mismatchedTenant(tenant))
        scan.append((databaseName, collectionName, collection, untagged, mismatched))
    return scan


def run(client, databases, tenant, apply, allowedMissing):
    collections = resolveCollections(client, databases, allowedMissing)
    scan = scanCollections(collections, tenant)
    totalUntagged = sum(item[3] for item in scan)
    totalMismatched = sum(item[4] for item in scan)

    for databaseName, collectionName, _, untagged, mismatched in scan:
        qualifiedName = f"{databaseName}.{collectionName}"
        if mismatched:
            print(f"  {AnsiColors.ERROR}BAD{AnsiColors.ENDC}  {qualifiedName}: "
                  f"{mismatched} document(s) belong to another or unknown tenant")
        elif untagged:
            mode = "SET" if apply else "DRY"
            color = AnsiColors.OK if apply else AnsiColors.WARNING
            print(f"  {color}{mode}{AnsiColors.ENDC}  {qualifiedName}: "
                  f"{untagged} document(s) untagged")
        else:
            print(f"  {AnsiColors.OK}OK{AnsiColors.ENDC}   {qualifiedName}: fully tagged as {tenant}")

    if totalMismatched:
        return totalUntagged, 0, totalMismatched

    totalTagged = 0
    if apply:
        for _, _, collection, untagged, _ in scan:
            if untagged:
                totalTagged += collection.update_many(
                    UNTAGGED, {"$set": {"tenantId": tenant}}).modified_count

    return totalUntagged, totalTagged, 0


def main():
    args = parseArguments()

    mongoHost = os.getenv("MONGO_HOST")
    if not mongoHost:
        print(f"{AnsiColors.ERROR}MONGO_HOST is not set: export it with the connection string of the "
              f"{args.tenant} database.{AnsiColors.ENDC}")
        return 1

    try:
        validateAccount(mongoHost, args.account_name, args.tenant)
        allowedMissing = validateAllowedMissing(args.allow_missing)
        client = MongoClient(mongoHost, serverSelectionTimeoutMS=10000)
        client.admin.command("ping")
        databases = selectedDatabases(args.database)

        if args.verify:
            print(f"Verifying tenant tagging (tenant: {args.tenant}, "
                  f"account: {args.account_name})\n")
            remaining, _, mismatched = run(
                client, databases, args.tenant, apply=False, allowedMissing=allowedMissing)
            if remaining == 0 and mismatched == 0:
                print(f"\n{AnsiColors.OK}Every inspected document belongs to {args.tenant}: the "
                      f"'or tenantId is null' read branch can be removed for these collections."
                      f"{AnsiColors.ENDC}")
                return 0
            if mismatched:
                print(f"\n{AnsiColors.ERROR}{mismatched} document(s) have a tenantId other than "
                      f"{args.tenant}; investigate before enabling strict isolation.{AnsiColors.ENDC}")
            if remaining:
                print(f"\n{AnsiColors.WARNING}{remaining} untagged document(s) remain: keep the "
                      f"'or tenantId is null' read branch.{AnsiColors.ENDC}")
            return 1

        mode = "APPLY" if args.apply else "DRY RUN"
        print(f"{mode} — tagging untagged documents as tenant {args.tenant} "
              f"in account {args.account_name}\n")

        untagged, tagged, mismatched = run(
            client, databases, args.tenant, args.apply, allowedMissing)

        if mismatched:
            print(f"\n{AnsiColors.ERROR}No documents were changed because {mismatched} document(s) "
                  f"already have a tenantId other than {args.tenant}.{AnsiColors.ENDC}")
            return 1

        if args.apply:
            print(f"\nTagged {tagged} document(s) as {args.tenant}.")
            if tagged != untagged:
                print(f"{AnsiColors.WARNING}Expected {untagged}; re-run to converge.{AnsiColors.ENDC}")
                return 1
        else:
            print(f"\nWould tag {untagged} document(s) as {args.tenant}. Re-run with --apply to write.")
        return 0
    except (PyMongoError, RuntimeError, ValueError) as e:
        print(f"{AnsiColors.ERROR}{e}{AnsiColors.ENDC}")
        return 2


if __name__ == "__main__":
    sys.exit(main())
