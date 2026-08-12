# Multitenant Data Isolation Architecture (Step 1)

## Required Architecture Inputs

- Requirements source: REQUIREMENTS.md (`apps/docs/Multitenant/Step_1/REQUIREMENTS.md`)
- System purpose: Once tenant and token are validated (Step 0), give each microservice a per-tenant data-isolation strategy for Cosmos DB, Azure Storage, the personal data vault, and outbound email, so a single shared backend deployment can serve both tenants without cross-tenant data exposure (REQUIREMENTS.md "Project purpose").
- Primary use cases: Tenant-scoped Cosmos DB read/write (SELC-8); tenant-scoped Azure Storage blob/file access (SELC-9); tenant-scoped personal data vault selection (SELC-10); tenant-scoped outbound email sender domain (SELC-11).
- Target users / actors: Backend microservices under `apps/` that persist data in Cosmos DB and/or Azure Storage, call the personal data vault, or send email; end users of `selfcare.pagopa.it` and `imprese.notifichedigitali.it` whose data must stay isolated (REQUIREMENTS.md "Primary users / actors").
- Runtime environment: Azure Container Apps, same shared Container App Environment target as Step 0 (`infra/core/_modules/container_app_environments`, `infra/resources/_modules/container_app_microservice`).
- Server framework: Quarkus 3.31.x on Java 17, reactive (SmallRye Mutiny), per repository convention; no change implied by REQUIREMENTS.md.
- Client framework: React (per Step 0 `ARCHITECTURE.md`); not otherwise relevant to this step's data-isolation scope.
- API style and integration model: REST over HTTP per existing repository convention; this step does not add new API surface, it changes how existing endpoints access data once tenant is already resolved (Step 0).
- Authentication and session model: Unchanged from Step 0 — tenant identity is already validated via `X-Tenant-Id` header reconciled with the JWT tenant claim before reaching the data-access layer (REQUIREMENTS.md "Authentication / roles"; SELC-8.5, SELC-9.5).
- Data model expectations: Azure Cosmos DB (Mongo API) as the primary datastore (`infra/core/_modules/cosmos_db/mongodb.tf`, `infra/resources/_modules/cosmosdb_database`, `infra/resources/_modules/cosmosdb_collection`), accessed via MongoDB/Panache reactive repositories per repository convention; Azure Storage accounts/containers for blobs and files (`infra/resources/document-ms/*/main.tf` shows per-purpose storage accounts, e.g. `documents_storage`, `user_attachments_storage`, with dedicated containers such as `STORAGE_CONTAINER_CONTRACT`, `STORAGE_CONTAINER_USER`); a personal data vault already referenced in code for PII (`apps/onboarding-ms/.../OnboardingController.java`, `apps/auth/.../OidcServiceImpl.java` — "Personal Data Vault"), provider/API details TO BE DECIDED; per-microservice isolation model (discriminator field vs. database-per-tenant for Cosmos DB; shared-container vs. per-tenant-account for Storage) is explicitly TO BE DECIDED per REQUIREMENTS.md SELC-8.6/SELC-9.6.
- Deployment model: Same Terraform-defined, environment-suffixed stacks as Step 0 (`-ar`/`-pnpg` today, consolidating toward `infra/resources/<app>/{dev,uat,prod}` per Step 0 `ARCHITECTURE.md`); this step does not change deployment topology, only what each deployment's data layer must do once shared.
- Scale expectations: TO BE DECIDED — not specified in REQUIREMENTS.md for this step; Step 0 `ARCHITECTURE.md` scale/noisy-neighbor concerns (APIM throttling, replica caps) apply upstream of this step but are not re-derived here.
- Security expectations: Tenant used for data isolation MUST be the same tenant already validated upstream (Step 0), never re-derived at the data-access layer (SELC-8.5, SELC-9.5); all isolation mechanisms MUST fail closed on an unresolved/unknown tenant (SELC-8.4, SELC-9.4, SELC-10.2, SELC-11.2) rather than default to another tenant's data, account, vault, or email domain.

## Initial Architecture (Provisional)

**Assumption A**: Tenant resolution and validation (header + JWT claim reconciliation) happen once, upstream of the data-access layer, per Step 0; this step only consumes that already-validated tenant value — it does not re-implement tenant resolution.

**Assumption B**: Each microservice's isolation-model choice (Cosmos DB, Storage) is made independently per service based on its own data-sensitivity and existing schema, not imposed platform-wide; REQUIREMENTS.md explicitly requires an inventory rather than a single blanket model.

1. **Cosmos DB tenant isolation (SELC-8)** — each microservice adopts exactly one of two models:
   - *Discriminator field*: a `tenantId` field is added to every document; every repository query/insert/update/delete path is required to include it. This is an addition to existing Panache repository methods, not a new datastore.
   - *Database-per-tenant*: a `TenantResolver` component maps the validated tenant to a specific Cosmos DB database/connection; unresolved tenants must fail closed (reject), never fall back to a default database.
   Which model applies to which microservice is **not decided** here (SELC-8.6) — this is an explicit inventory task, not an architectural default.

2. **Azure Storage tenant isolation (SELC-9)** — each microservice adopts exactly one of two models:
   - *Shared account, per-tenant container*: existing per-purpose storage accounts (e.g., `documents_storage`, `user_attachments_storage` in `document-ms`) gain a tenant dimension in container naming/selection; every blob operation must resolve to the tenant's container or be rejected.
   - *Per-tenant storage account*: a tenant-resolution mechanism analogous to the Cosmos DB `TenantResolver` selects the storage account; unresolved tenants must fail closed.
   Which model applies to which microservice is **not decided** here (SELC-9.6).

3. **Personal data vault tenant selection (SELC-10)** — microservices that already call the personal data vault (at least `onboarding-ms`, `auth`, per existing code references) must select the vault instance/tenant matching the validated tenant; unresolved tenant mapping must reject the call rather than default to another tenant's vault. Vault provider, API contract, and which other microservices integrate with it are **not decided** here (open question in REQUIREMENTS.md).

4. **Tenant-aware outbound email (SELC-11)** — microservices that send email (at least `institution-send-mail-scheduler`, per existing Terraform resources) must select the sender domain matching the validated tenant of the triggering request/workflow; unresolved tenant must block the send rather than use a default domain. Which other microservices send tenant-facing email is **not decided** here (SELC-11.3).

5. **Common constraint across all four areas** — none of these components independently re-derive tenant identity; they all consume the single tenant value already validated by the Step 0 header/claim reconciliation. This keeps tenant resolution as a single upstream concern and the data-layer components as pure consumers of that value.

**Unknowns kept visible (not guessed):**
- Per-microservice Cosmos DB isolation model assignment (SELC-8.6).
- Per-microservice Azure Storage isolation model assignment (SELC-9.6).
- Personal data vault provider/mechanism and its full list of integrating microservices (SELC-10.3).
- Full list of microservices sending tenant-facing email beyond `institution-send-mail-scheduler` (SELC-11.3).
- Source of truth for tenant → database/account/vault-tenant/email-domain mappings, and whether it is the same registry as Step 0's host/path routing mapping (Open Question).
- Migration/backfill approach for pre-existing single-tenant data (Open Question).
- Regulatory/data-residency constraints that might force a specific isolation model for a given tenant (Open Question).

## Requirement Traceability

| Architecture element | Requirement group | Notes |
|---|---|---|
| Cosmos DB discriminator field / database-per-tenant `TenantResolver` | SELC-8 | Per-service model assignment is an open inventory task (SELC-8.6) |
| Azure Storage per-tenant container / per-tenant storage account | SELC-9 | Per-service model assignment is an open inventory task (SELC-9.6); grounded in existing `document-ms` storage account/container pattern |
| Personal data vault tenant selection | SELC-10 | Vault provider and full integrating-service list are open (SELC-10.3); known callers today: `onboarding-ms`, `auth` |
| Tenant-aware outbound email sender domain | SELC-11 | Full sending-service inventory beyond `institution-send-mail-scheduler` is open (SELC-11.3) |
| Fail-closed behavior on unresolved tenant (all four areas) | SELC-8.4, SELC-9.4, SELC-10.2, SELC-11.2 | Consistent cross-cutting rule; no default-tenant fallback anywhere in the data layer |
| Reuse of Step 0 validated tenant value (no re-derivation) | SELC-8.5, SELC-9.5 | Ties this step's data-layer components to Step 0's tenant-resolution outcome, not a new resolution mechanism |

Requirements needing more architecture input before implementation: SELC-8.6 (Cosmos DB model per service), SELC-9.6 (Storage model per service), SELC-10.3 (vault provider + integrating services), SELC-11.3 (email-sending services beyond the known one), and the shared source-of-truth question for all four mappings.

## Dependency Rules

- Do not add a dependency when the standard library or a few lines of first-party code will do.
- Prefer zero new dependencies. If a library is required, justify it in the PR description.
- Only use libraries that are actively maintained (commit or release within the last 12 months).
- Only use the latest stable major version. No deprecated, abandoned, or pre-release packages.
- Reject any library with known unpatched CVEs. Check before adding and on every update.
- Audit transitive dependencies, not just direct ones. A small direct dep with a large or unvetted tree is a rejection.
- Pin exact versions with a committed lockfile. No floating ranges in production.
- Prefer libraries with a narrow scope, minimal dependencies of their own, and a clear security track record.
