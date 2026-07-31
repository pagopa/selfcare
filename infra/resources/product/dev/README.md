# product — unified (multitenant) dev stack

This folder replaces `dev-ar` and `dev-pnpg` with a single stack that serves both
tenants. It is **not applied and not wired into CI**; see "Why this is not in CI".

## Why `product` was converted second

`iam/dev` established the pattern. `product` was chosen next because it is the only
app in the consolidation scope that is simultaneously:

- a **leaf** — it calls no other internal microservice, so consolidating it cannot
  break a caller/callee ordering (`iam`, by contrast, calls `institution-ms`, so the
  reference conversion is not actually deployable first);
- **zero-divergence** — the two legacy stacks define exactly the same environment
  variable names, so no per-tenant behaviour has to be reconciled in application
  config;
- **not exposed through APIM** — it is an internal service only, so there is no API
  surface, no `service_url` to re-point and no frontend-visible change at all.

That makes it the smallest possible end-to-end exercise of the runbook below.

## What changes

| | `dev-ar` + `dev-pnpg` (today) | `dev` (this stack) |
|---|---|---|
| Container apps | `selc-d-product-ms` and `selc-d-pnpg-product-ms` | `selc-d-product-ms` only |
| Container App Environment | `selc-d-cae-002` and `selc-d-pnpg-cae-cp` | `selc-d-cae-002` only |
| Cosmos databases | `selcProduct` in the AR account **and** in the PNPG account | one `selcProduct` |
| Key Vault | `selc-d-kv` and the PNPG vault | `selc-d-kv` only |
| Terraform state | `...product.dev-ar.tfstate`, `...product.dev-pnpg.tfstate` | `...product.dev.tfstate` |

Two literal differences between the legacy stacks were resolved rather than
parameterised:

- `BLOB_STORAGE_CONTAINER_CONTRACT_TEMPLATE` already evaluated to the same value
  (`sc-d-documents-blob`) on both sides — contract templates are shared platform
  assets, not per-tenant data.
- `JAVA_TOOL_OPTIONS` carried extra JVM DNS flags on PNPG only
  (`-Djava.net.preferIPv4Stack=true -Dnetworkaddress.cache.ttl=30
  -Dnetworkaddress.cache.negative.ttl=1`). Those were workarounds for the PNPG
  container app environment, which this stack does not use, so they are dropped. If
  the merged app shows DNS caching symptoms in the AR environment, add them back
  here — do not re-introduce a per-tenant JVM configuration.

## The hard part is not Terraform: two catalogues become one

`product` is deliberately **excluded from tenant discrimination** (EPIC sub-task 6):
the product catalogue is global platform configuration, shared by every tenant. That
decision is what makes consolidation *possible* — but it also means the merge cannot
be done by tagging documents with a `tenantId` and copying them across.

Today there are two independent catalogues that have drifted apart. Merging them is a
**reconciliation**, and every difference has to be resolved by a human before the
cutover:

1. A `productId` present in only one catalogue — carried over as-is.
2. A `productId` present in both with identical configuration — deduplicated.
3. A `productId` present in both with **different** configuration (different roles,
   contract templates, `dataIsolation` block, back-office URLs, ...) — there is no
   automatic answer. Picking one silently changes behaviour for the tenant whose
   version lost, and the change is invisible in the Terraform diff.

Produce the three lists above *before* scheduling the cutover:

```bash
# against each account in turn
mongosh "<conn-string>" --eval '
  db.getSiblingDB("selcProduct").products
    .find({}, {productId:1, version:1, status:1, _id:0})
    .sort({productId:1, version:1})' > catalogue-<tenant>.json
diff catalogue-ar.json catalogue-pnpg.json
```

The `contractTemplates` collection has a **unique** index on
`(productId, name, version)`. Any product that exists in both catalogues with the
same template name and version will collide on import. Resolve case 3 first; the
import will otherwise fail partway through and leave a half-merged catalogue.

## Prerequisites

1. **Catalogue reconciliation complete** (above), with an explicit owner sign-off on
   every case-3 product.
2. **`JWT_PUBLIC_KEY` verifies both issuers** — `auth` for AR and `hub-spid-login`
   for PNPG. Each legacy stack held only its own key. If the two issuers do not share
   a key, every PNPG request to `product` fails authentication the moment traffic
   moves, and EPIC sub-task 4 has to land first.
3. **Callers reach the surviving instance.** `product` is consumed over the private
   DNS name of its container app. Every PNPG caller currently resolves
   `selc-d-pnpg-product-ms-ca.<pnpg-domain>`; those callers must be re-pointed at
   `selc-d-product-ms-ca.<ar-domain>` — which means they must be able to route to the
   AR container app environment. **Verify cross-environment connectivity before the
   cutover**; it is the single most likely thing to be missing, and it fails as a
   connection timeout in the caller, not as an error here.
4. **`product-cdc` follows.** It publishes the catalogue to blob storage for
   consumers of `libs/selfcare-onboarding-sdk-product`. It is an `-ar`-only
   deployment, so it does not need consolidating, but it must be pointed at the
   merged database in the same change or consumers keep reading the pre-merge
   snapshot.

## Cutover procedure

Nothing here creates infrastructure. Every resource already exists; this stack adopts
them. Applying before importing will attempt to create resources that exist and fail.

```bash
cd infra/resources/product/dev
terraform init

# 1. adopt the surviving AR resources
terraform import module.cosmosdb.azurerm_cosmosdb_mongo_database.this \
  "/subscriptions/<sub>/resourceGroups/selc-d-cosmosdb-mongodb-rg/providers/Microsoft.DocumentDB/databaseAccounts/selc-d-cosmosdb-mongodb-account/mongodbDatabases/selcProduct"
terraform import module.collection_products.azurerm_cosmosdb_mongo_collection.this "<.../collections/products>"
terraform import module.collection_contract_templates.azurerm_cosmosdb_mongo_collection.this "<.../collections/contractTemplates>"
terraform import module.container_app_product_ms.azurerm_container_app.this \
  "/subscriptions/<sub>/resourceGroups/selc-d-container-app-002-rg/providers/Microsoft.App/containerApps/selc-d-product-ms"

# 2. must be a no-op before anything else happens
terraform plan   # expected: "No changes."
```

A non-empty plan at step 2 means the stack and reality disagree — stop and reconcile
the difference rather than applying it.

```bash
# 3. merge the reconciled PNPG catalogue into the surviving database
#    (mongodump/mongorestore of the resolved documents only)

# 4. re-point PNPG callers, then verify

# 5. decommission
#    - terraform destroy in dev-pnpg, or delete the PNPG stack folder and state
#    - remove the PNPG Key Vault secrets that are now unused
```

## Verification

- `product` responds to a request carrying an AR-issued JWT **and** to one carrying a
  PNPG-issued JWT.
- The catalogue returned to a PNPG caller contains every product that PNPG saw before
  the merge, with the same configuration (this is what catches a case-3 product that
  was silently resolved the wrong way).
- `product-cdc` has republished the merged catalogue to blob storage and consumers
  read it.

## Rollback

Before step 3 the rollback is trivial: nothing has been mutated, so re-point callers
back and delete this state file.

After step 3 it is **not** trivial — the two catalogues have been merged into one
database. Take a Cosmos backup immediately before step 3 and treat restoring it as
the rollback path. This is the reason the catalogue reconciliation must be signed off
in advance rather than resolved during the cutover window.

## Why this is not in CI

`pr_product_infra.yml` does not plan this folder. Its state file does not exist yet,
so every pull request would render a "create 4 resources" plan for resources that must
be **imported**, not created — a permanently red plan that trains reviewers to approve
the wrong thing. The plan job belongs in the same change that performs the import.
