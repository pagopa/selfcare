# Tenant discriminator backfill

`backfill_tenant_id.py` sets the multitenant `tenantId` discriminator on the documents that were
created before the discriminator existed (Step_1 EPIC, sub-task 10).

## Why it is needed

The services read tenant-scoped data with `tenantId = <tenant> OR tenantId IS NULL`. The `IS NULL`
branch exists only so pre-existing data stayed visible the day the discriminator shipped, but it
also means an untagged document is readable by **both** tenants: until every document carries a
`tenantId` and that branch is removed from the queries, the Cosmos isolation is a convention, not a
boundary. This script closes that gap; removing the `IS NULL` branch from the services is the
follow-up that actually enforces it.

## What it touches

Only the databases and collections whose entities declare a `tenantId` field:

| Database | Collections |
|----------|-------------|
| `selcAuth` | `otpFlows` |
| `selcDocument` | `documents` |
| `selcIam` | `userClaims` |
| `selcMsCore` | `Institution`, `Delegations`, `MailNotification` |
| `selcOnboarding` | `onboardings`, `tokens` |
| `selcUser` | `userInfo`, `userInstitutions` |
| `selcUserGroup` | `UserGroups` |
| `selcWebhook` | `webhooks`, `webhookNotifications` |

The product catalogue database is deliberately excluded: product definitions are global and shared
between tenants, so tagging them would be wrong rather than merely useless. An expected collection
that is absent is a failure by default: otherwise a wrong or inaccessible host could verify
successfully without inspecting data. A collection known to be intentionally absent must be named
explicitly with `--allow-missing`.

## Safety properties

* **Dry run by default.** Nothing is written unless `--apply` is passed.
* **Never reassigns.** Only documents *without* a `tenantId` are updated, so a document already
  attributed to a tenant is left alone. Re-running the script, or running it for the wrong tenant
  after a correct run, cannot move data across tenants.
* **Validates the account before connecting.** `--account-name` must exactly match the account in
  `MONGO_HOST`, and that account name must belong to `--tenant` according to the platform naming
  convention (`...-pnpg-cosmosdb-mongodb-account` for PNPG, the non-PNPG form for AR).
* **Fails before writing on inconsistent data.** If any inspected document already has a non-null
  `tenantId` different from the requested tenant, the whole run is rejected and no collection is
  modified.
* **Verifies real coverage.** Collection discovery errors, unexpected missing collections and a run
  that inspects zero collections all fail; they cannot produce a successful `--verify`.
* **Interruptible.** Each collection converges independently; re-running resumes where it stopped.

## Usage

The tenant is an explicit argument rather than something inferred from the data: each tenant has its
own Cosmos DB account, so it is the connection string that decides which tenant you are migrating,
and stating it explicitly is what turns a wrong `MONGO_HOST` into a mismatch you notice instead of a
mislabelled database.

```bash
export MONGO_HOST="<connection string of the tenant's mongo/cosmos account>"
ACCOUNT_NAME="selc-d-cosmosdb-mongodb-account"

# 1. See what would be tagged
python3 backfill_tenant_id.py --tenant AR --account-name "$ACCOUNT_NAME"

# 2. Write
python3 backfill_tenant_id.py --tenant AR --account-name "$ACCOUNT_NAME" --apply

# 3. Confirm nothing is untagged or attributed to another tenant
python3 backfill_tenant_id.py --tenant AR --account-name "$ACCOUNT_NAME" --verify
```

Run the three steps once per tenant, against that tenant's own connection string.

## Options

* `--tenant AR|PNPG` (required) — tenant owning the data reachable through `MONGO_HOST`.
* `--account-name <name>` (required) — expected Cosmos account. It must match both `MONGO_HOST` and
  the selected tenant; obtain it from `module.local.config.tenant_data_isolation`.
* `--database <name>` — restrict to one database; repeat the flag for several.
* `--allow-missing <database.collection>` — explicitly allow one expected collection to be absent;
  repeatable. This is deliberately per collection rather than a broad “ignore missing” switch.
* `--apply` — perform the updates.
* `--verify` — report only, exiting non-zero while untagged, mismatched, inaccessible or unexpectedly
  absent data remains. This is the gate for dropping the `or tenantId is null` branch from the
  services: it must exit `0` for both tenants in an environment before that environment's reads can
  be made strict.

Requires `pymongo` (`pip install pymongo`).

## After the backfill: turning isolation strict

Tagging the data is only half the job. Until the services stop accepting untagged documents, an untagged
document that appears later — from a restored backup, a replayed event, a fixture — is still readable by both
tenants. Once `--verify` exits `0` for both tenants in an environment, set in `infra/resources/_modules/local-env/locals.tf`:

```hcl
strict_tenant_data_isolation = true
```

and apply that environment. The value is per environment, in one place, because it describes the state of the
environment's *data*, not of a service: each stack that carries tenant-scoped data passes
`module.local.config.strict_tenant_data_isolation` to the `container_app_microservice` module, which injects
`SELFCARE_TENANT_STRICT_DATA_ISOLATION` (property `selfcare.tenant.strict-data-isolation`; the same variable
name works for both the Quarkus and the Spring services).

It drops the `or tenantId is null` branch in `auth`, `document-ms`, `iam`,
`user-ms`, `onboarding-ms`, `user-group-ms`, `institution-ms`, `delegation-cdc` and `user-cdc` — every
service that has one, so that an environment flipping the variable becomes strict everywhere at once. Flipping
only some of them would produce an environment that reports isolation it does not have.

It is a flag rather than a code deletion because the backfill lands at a different time in each environment:
a hardcoded switch would keep the strict build out of PROD until PROD had been migrated. Each environment
flips when its own data is ready, and reverts by setting the local back to `false`. It defaults to `false`,
so deploying the services changes nothing until the environment is switched.

The flag is temporary. Once every environment runs strict, delete both the flag and the `or tenantId is null`
branch, so isolation holds by construction rather than by configuration (Step_1/EPIC.md sub-task 2).

Note that the flag does not affect callers with no resolvable tenant — schedulers and event consumers still
read unscoped. Closing that gap is a separate decision from the backfill.
