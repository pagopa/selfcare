#!/usr/bin/env python3
"""Backfill the multitenant `tenantId` discriminator on pre-existing documents.

Step_1 EPIC sub-task 10 (apps/docs/Multitenant/Step_1/EPIC.md).

Every tenant-scoped read currently matches `tenantId = <tenant> OR tenantId IS NULL`, because a
strict filter would have hidden every pre-existing document from both tenants the day it shipped.
That `OR` also means an untagged document is readable by *both* tenants, so the isolation is not a
security boundary until this backfill has run everywhere and the branch has been removed from the
services.

The tenant is taken from the argument, not guessed from the data: today each tenant has its own
Cosmos DB account, so the account MONGO_HOST points at is what determines the tenant, and stating
it explicitly is what keeps a wrong connection string from silently mislabelling a whole database.

Two invariants make the script safe to re-run and safe to interrupt:
  * only documents WITHOUT a `tenantId` are touched, so a document already attributed to a tenant is
    never reassigned — a replay, or a run with the wrong tenant argument after a correct one, cannot
    move data between tenants;
  * nothing is written unless --apply is passed.
"""

import argparse
import os
import sys

from pymongo import MongoClient
from pymongo.errors import PyMongoError

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

UNTAGGED = {"tenantId": {"$exists": False}}


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
    parser.add_argument("--database", action="append", choices=sorted(TENANT_SCOPED_COLLECTIONS),
                        help="Restrict to one database; repeatable. Defaults to every listed database.")
    parser.add_argument("--apply", action="store_true",
                        help="Actually write. Without it the script only reports what it would tag.")
    parser.add_argument("--verify", action="store_true",
                        help="Report how many documents are still untagged and exit. Use this to decide "
                             "whether the 'or tenantId is null' read branch can be dropped.")
    return parser.parse_args()


def selectedDatabases(requested):
    return requested if requested else sorted(TENANT_SCOPED_COLLECTIONS)


def backfillCollection(collection, tenant, apply):
    untagged = collection.count_documents(UNTAGGED)
    if untagged == 0 or not apply:
        return untagged, 0
    result = collection.update_many(UNTAGGED, {"$set": {"tenantId": tenant}})
    return untagged, result.modified_count


def existingCollections(database):
    try:
        return set(database.list_collection_names())
    except PyMongoError as e:
        print(f"{AnsiColors.ERROR}Cannot list collections of {database.name}: {e}{AnsiColors.ENDC}")
        return set()


def run(client, databases, tenant, apply):
    totalUntagged = 0
    totalTagged = 0
    missing = []

    for databaseName in databases:
        database = client[databaseName]
        present = existingCollections(database)

        for collectionName in TENANT_SCOPED_COLLECTIONS[databaseName]:
            if collectionName not in present:
                # Every database is reachable from one connection string, but a given deployment only
                # owns some of them; a missing collection is expected here, not an error.
                missing.append(f"{databaseName}.{collectionName}")
                continue

            untagged, tagged = backfillCollection(database[collectionName], tenant, apply)
            totalUntagged += untagged
            totalTagged += tagged

            if untagged == 0:
                print(f"  {AnsiColors.OK}OK{AnsiColors.ENDC}   {databaseName}.{collectionName}: fully tagged")
            elif apply:
                print(f"  {AnsiColors.OK}SET{AnsiColors.ENDC}  {databaseName}.{collectionName}: "
                      f"tagged {tagged}/{untagged} document(s) as {tenant}")
            else:
                print(f"  {AnsiColors.WARNING}DRY{AnsiColors.ENDC}  {databaseName}.{collectionName}: "
                      f"{untagged} document(s) still untagged")

    if missing:
        print(f"\n{AnsiColors.WARNING}Not present on this host (skipped): {', '.join(missing)}{AnsiColors.ENDC}")

    return totalUntagged, totalTagged


def main():
    args = parseArguments()

    mongoHost = os.getenv("MONGO_HOST")
    if not mongoHost:
        print(f"{AnsiColors.ERROR}MONGO_HOST is not set: export it with the connection string of the "
              f"{args.tenant} database.{AnsiColors.ENDC}")
        return 1

    client = MongoClient(mongoHost)
    databases = selectedDatabases(args.database)

    if args.verify:
        print(f"Verifying tenant tagging (expected tenant: {args.tenant})\n")
        remaining, _ = run(client, databases, args.tenant, apply=False)
        if remaining == 0:
            print(f"\n{AnsiColors.OK}No untagged documents left: the 'or tenantId is null' read branch "
                  f"can be removed for these collections.{AnsiColors.ENDC}")
            return 0
        print(f"\n{AnsiColors.WARNING}{remaining} untagged document(s) remain: keep the "
              f"'or tenantId is null' read branch.{AnsiColors.ENDC}")
        return 1

    mode = "APPLY" if args.apply else "DRY RUN"
    print(f"{mode} — tagging untagged documents as tenant {args.tenant}\n")

    untagged, tagged = run(client, databases, args.tenant, args.apply)

    if args.apply:
        print(f"\nTagged {tagged} document(s) as {args.tenant}.")
        if tagged != untagged:
            print(f"{AnsiColors.WARNING}Expected {untagged}; re-run to converge.{AnsiColors.ENDC}")
            return 1
    else:
        print(f"\nWould tag {untagged} document(s) as {args.tenant}. Re-run with --apply to write.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
