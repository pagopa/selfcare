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
between tenants, so tagging them would be wrong rather than merely useless. Collections listed above
but absent from the host being migrated are reported as skipped, not as failures.

## Safety properties

* **Dry run by default.** Nothing is written unless `--apply` is passed.
* **Never reassigns.** Only documents *without* a `tenantId` are updated, so a document already
  attributed to a tenant is left alone. Re-running the script, or running it for the wrong tenant
  after a correct run, cannot move data across tenants.
* **Interruptible.** Each collection converges independently; re-running resumes where it stopped.

## Usage

The tenant is an explicit argument rather than something inferred from the data: each tenant has its
own Cosmos DB account, so it is the connection string that decides which tenant you are migrating,
and stating it explicitly is what turns a wrong `MONGO_HOST` into a mismatch you notice instead of a
mislabelled database.

```bash
export MONGO_HOST="<connection string of the tenant's mongo/cosmos account>"

# 1. See what would be tagged
python3 backfill_tenant_id.py --tenant AR

# 2. Write
python3 backfill_tenant_id.py --tenant AR --apply

# 3. Confirm nothing is left untagged
python3 backfill_tenant_id.py --tenant AR --verify
```

Run the three steps once per tenant, against that tenant's own connection string.

Options:

* `--tenant AR|PNPG` (required) — tenant owning the data reachable through `MONGO_HOST`.
* `--database <name>` — restrict to one database; repeat the flag for several.
* `--apply` — perform the updates.
* `--verify` — report only, exiting non-zero while untagged documents remain. This is the gate for
  dropping the `or tenantId is null` branch from the services: it must exit `0` for both tenants in
  an environment before that environment's reads can be made strict.

Requires `pymongo` (`pip install pymongo`).
