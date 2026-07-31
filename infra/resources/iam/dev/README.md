# iam — unified multitenant stack (dev)

Reference implementation of **EPIC sub-task 7, deployment consolidation**: one deployment of `iam-ms`
serving both tenants (`AR`, `PNPG`) instead of the separate `dev-ar` and `dev-pnpg` stacks.

`iam` was chosen as the first app to convert because its two legacy stacks differ only in naming, DNS
and one JVM flag — no divergent business configuration — so the consolidation pattern can be
established without simultaneously resolving app-specific differences.

## What this stack does

| Concern | Legacy (`dev-ar` + `dev-pnpg`) | Here |
| --- | --- | --- |
| Container app | 2 (`selc-d-iam-ms`, `selc-d-pnpg-iam-ms`), in 2 different Container App Environments | 1 (`selc-d-iam-ms`) |
| Cosmos database | 2 accounts, `selcIam` in each | 1 database, tenants separated by the `tenantId` discriminator |
| Key Vault | `selc-d-kv` and `selc-d-pnpg-kv` | `selc-d-kv` |
| APIM APIs | 2, each pointing at its own backend | **still 2**, both pointing at the single backend |
| Terraform state | `...iam.dev-ar.tfstate`, `...iam.dev-pnpg.tfstate` | `...iam.dev.tfstate` |

**The two APIM APIs are kept on purpose.** The PNPG frontend calls
`api-pnpg.dev.selfcare.pagopa.it/imprese/iam` and the AR frontend calls
`api.dev.selfcare.pagopa.it/iam`. Keeping both URLs means this consolidation is invisible to both
frontends and can be rolled back by repointing one `service_url`. Collapsing the two URLs into one is
a separate, frontend-visible decision and is intentionally out of scope.

Tenant identity does not depend on which API is called: each API resolves `X-Tenant-Id` from the
calling origin against the `tenant_ids` registry and discards any `X-Tenant-Id` the caller supplied
(see `_modules/apim_api`). A PNPG browser reaching the AR API is still resolved as `PNPG`.

## This is a successor stack, not a parallel one

The EPIC describes standing the unified stacks up "alongside" the legacy ones. That is not literally
possible here: a genuinely parallel stack would have to create a *second* container app and a *third*
and *fourth* APIM API, which is more moving parts to keep in sync than the migration it is meant to
de-risk. Instead this stack **adopts** the existing AR resources and the existing PNPG APIM API into a
new state file. Nothing is destroyed and recreated; the rollback path is re-pointing state, not
rebuilding infrastructure.

Consequence: **do not `terraform apply` this stack before running the import step below.** Without it,
Terraform sees an empty state and tries to create resources that already exist.

## Prerequisites — must be true before cutover

These are hard blockers, not checklist items. Each one, if skipped, produces a silent data or auth
failure rather than an apply error.

1. **Sub-task 6 tenant discriminator is deployed and the backfill has run.** Every `userClaims`
   document in both databases must carry a `tenantId`. Merging two databases whose documents are
   untagged produces one database in which no query can tell the tenants apart — and the current
   migration-phase read filter (`tenantId == current OR tenantId == null`) would show *every* legacy
   PNPG document to AR users and vice versa. The `OR tenantId == null` branch must be removed before,
   not after, the merge.

2. **The `userClaims` unique index on `email` is composite with `tenantId`.** It is declared that way
   in `iam.tf`, but Cosmos will reject the index change if duplicates already exist, and reject the
   data import if the index is still globally unique. Apply the index change first, on the empty-of-
   PNPG-data collection, then import.

3. **Data migration executed.** Copy `userClaims` from the PNPG Cosmos account into this database with
   `tenantId: "PNPG"` stamped, and verify counts on both sides. `roles` is a global catalogue and is
   *not* merged — confirm the two accounts' `roles` collections are equivalent first; if they have
   diverged, that divergence is a product decision to resolve before, not during, cutover.

4. **`JWT_PUBLIC_KEY` verifies both issuers.** AR sessions are signed by the `auth` microservice, PNPG
   sessions by `hub-spid-login`. Each legacy stack held only its own key, so a single-valued
   `JWT_PUBLIC_KEY` is correct only if both issuers share a key. If they do not, multi-issuer
   verification (EPIC sub-task 4) must land first; otherwise every PNPG request fails authentication
   the moment traffic moves.

5. **Secrets present in `selc-d-kv`.** Every secret named in `secrets_names_iam_ms` must exist in the
   AR Key Vault, and its value must be the one that is correct for both tenants. Any secret that is
   genuinely tenant-specific must not be collapsed — it needs a per-tenant env var and app-side
   selection by tenant.

6. **Network reachability.** The PNPG APIM instance must be able to reach the AR container app
   environment's private DNS zone (`whitemoss-eb7ef327.westeurope.azurecontainerapps.io`). APIM is
   shared per environment (`selc-d-apim-v2`), so this is expected to already hold, but it must be
   verified with an actual request before cutover, not assumed.

## Cutover procedure

```bash
cd infra/resources/iam/dev
terraform init

# 1. Adopt the AR resources. Get the ids from the legacy state rather than the portal:
#      cd ../dev-ar && terraform state list && terraform state show <addr>
terraform import module.container_app_iam_ms.<resource> <azure-resource-id>
terraform import module.cosmosdb.<resource>              <azure-resource-id>
terraform import module.collection_iam_user.<resource>   <azure-resource-id>
terraform import module.collection_iam_roles.<resource>  <azure-resource-id>
terraform import module.apim_api_ar.<resource>           <azure-resource-id>

# 2. Adopt the PNPG APIM API (from the dev-pnpg state).
terraform import module.apim_api_pnpg.<resource>         <azure-resource-id>

# 3. The plan must now be EMPTY except for the two intended changes:
#      - module.apim_api_pnpg service_url  -> the AR container app
#      - module.collection_iam_user index  -> composite (tenantId, email)
#    Anything else in the plan means an import was missed. Do not apply past it.
terraform plan

terraform apply
```

The exact resource addresses are deliberately not hard-coded here: they depend on the module versions
pinned at cutover time, and a stale copied list is worse than no list. `terraform state list` in the
legacy stack is the source of truth.

### Verification before decommissioning anything

- AR frontend login and an authenticated `iam` call succeed.
- PNPG frontend login and an authenticated `iam` call succeed, and land on the **same** container app
  (check Application Insights `cloud_RoleInstance`).
- A PNPG user cannot read AR claims and vice versa — verified against real data, not by inspection.
- The legacy PNPG container app receives zero requests for a full business day.

### Rollback

Before step 3's apply, rollback is `terraform apply` in `dev-pnpg` (nothing has changed).
After it, rollback is re-pointing `module.apim_api_pnpg`'s `service_url` back to the PNPG container
app, which must be left running and un-decommissioned until the verification window closes.

### Decommissioning

Only after the verification window: delete the `dev-pnpg` stack folder, its state file, its container
app, its Cosmos database and its Key Vault secrets — in that order, and as a separate change so it can
be reviewed on its own.

## Why this stack is not wired into CI yet

`.github/workflows/pr_iam_infra.yml` deliberately does **not** plan this folder. Its state file does
not exist yet, so `terraform plan` would report "create 12 resources" on every pull request — a
permanently red-looking plan for resources that already exist and must be imported, not created. That
is a standing invitation to approve the wrong thing.

Add the plan job in the same change that performs the import, not before.

## Status

This folder is the **reference conversion**. It is written, formatted, `terraform validate`-clean and
reviewed, but **not applied to any environment**, not imported, and not wired into CI. The remaining
apps and environments are listed in EPIC.md sub-task 7.
