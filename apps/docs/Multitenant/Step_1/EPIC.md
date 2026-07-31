# Jira Epic — Multitenant Data Isolation Rollout (Step 1)

Source inputs: `REQUIREMENTS.md`, `ARCHITECTURE.md`, `SECURITY.md` (this folder), and
`apps/docs/Multitenant/Step_0/*` (prerequisite tenant identification/validation).

## Epic

**Title:** Multitenant data isolation across Cosmos DB, Azure Storage, personal data vault, and email

**Description:**
Once a request's tenant and token are identified and validated (Step 0), each microservice must isolate the
tenant's data correctly at the data layer. This epic covers choosing and implementing, per microservice, one
of two Cosmos DB isolation models (discriminator field or database-per-tenant) and one of two Azure Storage
isolation models (shared account/per-tenant container or per-tenant storage account), selecting the correct
personal data vault tenant, and selecting the correct outbound email sender domain — all driven by the tenant
already validated upstream, with fail-closed behavior everywhere a tenant cannot be resolved.

**Goal / business value:** Allow a single shared backend deployment to serve both tenants
(`selfcare.pagopa.it` / AR, `imprese.notifichedigitali.it` / PNPG) without cross-tenant data leakage in
storage, PII handling, or communications.

**Primary requirement groups covered:** SELC-8, SELC-9, SELC-10, SELC-11 (`REQUIREMENTS.md`).

**Dependencies:** Requires Step 0's tenant resolution/validation (`X-Tenant-Id` header reconciled with the JWT
tenant claim) to already be in place and trustworthy before any data-layer component can consume it.

**Definition of Done (epic-level):**
- Every microservice has a documented, single (non-mixed) Cosmos DB isolation model and a documented, single
  Azure Storage isolation model.
- No data-access path (Cosmos DB, Storage, personal data vault, email) can silently fall back to a default or
  wrong tenant; every unresolved-tenant case is rejected, not defaulted.
- Personal data vault calls and outbound email use the tenant-correct vault instance and sender domain
  respectively, for every microservice that performs these operations.
- Still-open items (per-service model assignment, vault provider, full email-sending service inventory,
  shared mapping source of truth, data migration/backfill approach, regulatory constraints) are resolved or
  explicitly descoped before epic closure.

**Out of scope:** Tenant resolution/validation itself (covered by Step 0); frontend changes; fine-grained
authorization model beyond tenant scoping.

---

## Sub-tasks

### 1. Inventory Cosmos DB isolation model per microservice
- **Maps to:** SELC-8.1, SELC-8.2, SELC-8.6
- **Description:** For each microservice persisting data in Cosmos DB, decide and document whether it uses
  the discriminator-field model or the database-per-tenant model. Confirm no microservice mixes both models
  for the same data entity.
- **Acceptance criteria:** A documented per-microservice mapping (service → isolation model) exists and is
  reviewed; no microservice is left undecided.
- **Blockers/open questions:** Model choice may depend on regulatory/data-residency constraints (still `TO BE
  DECIDED`).
- **Status: done.** Inventory and decision live in `Step_0/EPIC.md` sub-task 6, which ran ahead of this
  document: all 15 Mongo-using services (`auth`, `delegation-cdc`, `document-ms`, `iam`,
  `institution-send-mail-scheduler`, `onboarding-cdc`, `onboarding-functions`, `onboarding-ms`, `product`,
  `product-cdc`, `user-cdc`, `user-group-cdc`, `user-ms`, `webhook`, `user-group-ms`) use the
  **discriminator-field** model. No service mixes both. `product`/`product-cdc` are deliberately unscoped —
  they hold the global product catalogue, which is not tenant data. The regulatory blocker is unchanged: no
  constraint forcing physical separation has been identified, so revisit if that changes.

### 2. Implement discriminator-field isolation where selected
- **Maps to:** SELC-8.1, SELC-8.3, SELC-8.5
- **Description:** For microservices assigned the discriminator-field model (sub-task 1), add a `tenantId`
  field to affected documents and update every Panache/Mongo repository method (read/write/update/delete) to
  include the validated tenant in its filter.
- **Acceptance criteria:** No repository method can execute without a tenant filter; code review checklist
  updated to flag any missing filter as a defect (`SECURITY.md` Cosmos DB rules).
- **Depends on:** Sub-task 1.
- **Status: rolled out to every Mongo-using service, and now switchable to enforcement per environment.** The
  filter is applied inside the data-access layer (never as an opt-in variant): `TenantDataIsolation` in
  `institution-ms`, `QueryUtils`/persistence helpers in `onboarding-ms` and `user-ms`, repository defaults
  elsewhere; CDC apps and the scheduler propagate the source `tenantId` instead of filtering, having no
  request context. Reads still match `tenantId = ? or tenantId is null` by default, so pre-existing untagged
  documents stay visible during the migration window. That branch is now controlled by
  `selfcare.tenant.strict-data-isolation` (env var `SELFCARE_TENANT_STRICT_DATA_ISOLATION`, default `false`)
  at all fourteen chokepoints, across every service that has one: `auth`, `document-ms`, `iam` (entity and
  aggregation paths), `user-ms` (query builder and two service-level lookups), `onboarding-ms`,
  `user-group-ms`, `institution-ms`, `delegation-cdc` (both repositories) and `user-cdc`. It is a flag rather
  than a deletion because the backfill lands at a different time in each environment: a hardcoded switch would
  keep the strict build out of PROD until PROD had been migrated, holding every unrelated change behind a data
  migration.
  Each environment turns strict when its own `--verify` is clean, and reverts without a deployment.
  **Remaining:** run the backfill everywhere, flip the flag everywhere, then delete both the flag and the
  `or tenantId is null` branch — only after that last step is this enforcement by construction rather than by
  configuration. The unscoped path for callers with no resolvable tenant (schedulers, consumers) is untouched
  by the flag and remains a separate open decision.

### 3. Implement database-per-tenant isolation where selected
- **Maps to:** SELC-8.1, SELC-8.4, SELC-8.5
- **Description:** For microservices assigned the database-per-tenant model (sub-task 1), implement a
  `TenantResolver` that routes to the correct Cosmos DB database/connection based on the validated tenant, and
  fails closed (rejects) when the tenant cannot be mapped to a known database.
- **Acceptance criteria:** Requests for an unmapped tenant are rejected, not routed to a default database;
  connection strings/keys are Key Vault-backed (`infra/core/_modules/key_vault`).
- **Depends on:** Sub-task 1.
- **Status: not applicable as written — no service selected this model (sub-task 1).** What was built instead is
  an orthogonal, product-driven axis: `ProductDatabaseResolver` + `OnboardingMongoDatabaseResolver` +
  `ProductRoutingContext` in `onboarding-ms` route to a per-product database declared by the `product` service
  (`DatabaseIsolationModel`), because the driver there is contractual rather than tenant-related. It is
  fail-closed in the same sense this sub-task requires (unknown product, lookup failure, or `DEDICATED` without
  a database name raises rather than falling back to the shared database), and a dedicated database still
  carries the `tenantId` discriminator, so the two axes compose. If a tenant ever needs its own database, the
  per-tenant coordinates it would need (`cosmos_account_name`, `cosmos_resource_group_name`,
  `cosmos_connection_string_secret_name`) are already in the sub-task 9 registry, Key Vault-backed by secret
  name. Known gap carried from `Step_0/EPIC.md`: ~12 `onboarding-ms` endpoints identified only by
  `onboardingId` cannot resolve a dedicated database and work today only because every product is `SHARED`.

### 4. Inventory Azure Storage isolation model per microservice
- **Maps to:** SELC-9.1, SELC-9.2, SELC-9.6
- **Description:** For each microservice persisting data in Azure Storage, decide and document whether it
  uses the shared-account/per-tenant-container model or the per-tenant storage account model. Start from known
  storage users (`document-ms`: `documents_storage`, `user_attachments_storage`) and extend to other services.
- **Acceptance criteria:** A documented per-microservice mapping (service → isolation model) exists; no
  microservice is left undecided or mixing both models.
- **Depends on:** none (can run in parallel with sub-tasks 1–3).
- **Status: done, with one service still undecided.** Recorded in `Step_0/EPIC.md` sub-task 6: `document-ms`
  (contracts/attachments) is **in scope** with the **shared account, tenant discriminated at container/path
  level** model; `product` (contract templates) and `registry-proxy-runner` (public registry cache) are **out
  of scope** as non-tenant data; `product-cdc`/`user-cdc` archive through the shared
  `selfcare-onboarding-sdk-azure-storage` lib and inherit upstream tenant marking. `dashboard-bff`
  (institution logos) is still **TO BE DECIDED** — the one open entry.

### 5. Implement per-tenant container isolation where selected
- **Maps to:** SELC-9.1, SELC-9.3, SELC-9.5
- **Description:** For microservices assigned the shared-account/per-tenant-container model (sub-task 4),
  derive the container to use only from the validated tenant (never from client-supplied input) for every
  blob/file operation; reject operations that cannot resolve a container.
- **Acceptance criteria:** No blob/file operation can default to a fallback container; least-privilege
  SAS/managed-identity scope preserved per tenant.
- **Depends on:** Sub-task 4.
- **Status: not started; the mapping half is ready, the service half is not.** Sub-task 9 provides
  `TenantDataIsolationRegistry.storageContainer(tenant, baseContainer)`, which appends the tenant's registered
  suffix and throws instead of returning a base name when the tenant is unresolved. Nothing consumes it yet:
  `document-ms` still binds one container per `StorageOrigin` at `@PostConstruct` (`StorageRegistry`), so its
  21 `clientFor(...)` call sites (18 in `DocumentContentServiceImpl`, 3 in `DocumentServiceImpl`) are
  tenant-blind.
  - **Blocker found while scoping this:** the blob containers themselves are not declared in this repository —
    `infra/resources/document-ms/*/main.tf` only reads the storage accounts through `data` blocks, and the
    container names arrive as literal env values (`sc-<e>-documents-blob`, `sc-<e>-usrattach-blob`). The
    per-tenant containers must therefore be provisioned wherever those are owned before this sub-task can
    ship, otherwise every PNPG blob operation fails closed with no in-repo fix available.
  - **Design note for whoever picks this up:** `StorageRegistry` must cache a client per (origin, tenant), and
    each call site needs classifying first — the SYSTEM client is used both for tenant data (contracts,
    attachments) and for shared platform assets (mail/contract templates), and only the former may be
    tenant-suffixed. Blindly suffixing every call site would move shared templates into a per-tenant container
    that does not contain them.

### 6. Implement per-tenant storage account isolation where selected
- **Maps to:** SELC-9.1, SELC-9.4, SELC-9.5
- **Description:** For microservices assigned the per-tenant storage account model (sub-task 4), implement
  tenant-to-account resolution analogous to the Cosmos DB `TenantResolver`, failing closed on an unresolved
  tenant.
- **Acceptance criteria:** Requests for an unmapped tenant are rejected, not routed to a default storage
  account.
- **Depends on:** Sub-task 4.
- **Status: not applicable — no service selected this model (sub-task 4).** If one does, the per-tenant
  account naming infix (`ar` / `pnpg`, matching the existing `sc<e><loc>ar…st01` / `sc<e><loc>pnpg…st01`
  accounts) is already in the sub-task 9 registry and `storageAccountInfix()` fails closed on an unmapped
  tenant.

### 7. Personal data vault tenant selection
- **Maps to:** SELC-10.1, SELC-10.2, SELC-10.3
- **Description:** Identify the personal data vault provider/API contract and the full list of integrating
  microservices (known callers today: `onboarding-ms`, `auth`). Implement tenant-to-vault-instance selection
  using the already-validated tenant, rejecting calls for unmapped tenants.
- **Acceptance criteria:** Every vault call uses the tenant-correct vault instance; unmapped tenants are
  rejected, not defaulted; vault responses are never logged as raw PII.
- **Blockers/open questions:** Vault provider/mechanism still `TO BE DECIDED`.
- **Status: blocked on the provider decision.** The registry slot exists and is deliberately empty:
  `personal_data_vault_tenant` is `null` for both tenants, and `personalDataVaultTenant()` throws rather than
  defaulting (SELC-10.2). Once the provider and per-tenant instance identifiers are known, filling this in is a
  change to `local.tenant_data_isolation` alone, plus wiring the callers.

### 8. Tenant-aware outbound email sender domain
- **Maps to:** SELC-11.1, SELC-11.2, SELC-11.3
- **Description:** Identify the full list of microservices sending tenant-facing email (known:
  `institution-send-mail-scheduler`) and implement tenant-to-sender-domain selection using the already-validated
  tenant; block sends when the tenant cannot be resolved for the trigger.
- **Acceptance criteria:** No email is sent with a mismatched or default tenant's sender domain; unresolved
  tenant blocks the send with an explicit error, not a silent default.
- **Blockers/open questions:** Full list of email-sending services beyond the known one still `TO BE DECIDED`.
- **Status: blocked on the sender-domain table.** Same shape as sub-task 7: `email_sender_domain` is `null` per
  tenant and `emailSenderDomain()` throws. Note that the sender address is a Key Vault secret today
  (`MAIL_SENDER_ADDRESS` → `smtp-usr`), so the per-tenant *domain* is new non-secret config, while the SMTP
  credential stays in Key Vault. `institution-send-mail-scheduler` already carries `tenantId` on
  `MailNotification` and resolves a tenant for its machine token, so the trigger-side tenant needed here exists.

### 9. Define shared tenant-mapping source of truth
- **Maps to:** Open Question (source of truth for tenant → database/account/vault-tenant/email-domain)
- **Description:** Decide whether the Cosmos DB, Storage, vault, and email tenant mappings reuse the same
  registry defined in Step 0 (SELC-6.3, host/path routing) or use a separate one; implement as non-secret
  routing/config data, separate from actual credentials (Key Vault references only).
- **Acceptance criteria:** Single documented source of truth referenced by sub-tasks 1–8; no mapping data
  duplicated ad hoc per microservice.
- **Depends on:** none directly, but should be resolved early since sub-tasks 1–8 consume it.
- **Status: implemented (registry and consumption mechanism; two dimensions intentionally empty).**
  **Decision: the same registry as Step 0**, not a second one. `local.tenant_data_isolation` lives in
  `infra/resources/_modules/local-env/locals.tf` directly next to `local.tenant_frontend_origins` /
  `local.tenant_ids`, so one file declares everything a tenant *is* — its frontend origin (routing,
  Step 0) and its data-layer resources (Step 1). A second registry would have made it possible for
  the two to disagree about which tenants exist.
  - **What the registry holds, per tenant:** `cosmos_account_name`, `cosmos_resource_group_name`,
    `cosmos_connection_string_secret_name`, `storage_account_infix`, `storage_container_suffix`,
    `personal_data_vault_tenant`, `email_sender_domain`. Values are the names the existing
    `-ar`/`-pnpg` stacks already use (`selc-<e>-cosmosdb-mongodb-account` vs
    `selc-<e>-weu-pnpg-cosmosdb-mongodb-account`; storage accounts `sc<e><loc>ar…st01` vs
    `sc<e><loc>pnpg…st01`), so this is a re-declaration of current infrastructure, not a new naming
    scheme. `local.mongo_db` picks one of the two by deployment folder; the registry exposes both
    unconditionally, because a consolidated deployment serves both tenants and can no longer pick by
    folder.
  - **Secrets stay out.** The registry carries the Key Vault secret *name* of a connection string,
    never its value (`Step_1/SECURITY.md`, secret handling). PNPG's entry uses a suffixed name
    (`mongodb-connection-string-pnpg`) because after consolidation both tenants' secrets live in the
    surviving stack's single Key Vault, where the bare name is already taken by AR.
  - **`personal_data_vault_tenant` and `email_sender_domain` are `null` on purpose.** The vault
    provider (SELC-10.3) and the per-tenant sender domain table (SELC-11.3) are still open, and
    guessing a value would be exactly the silent default SELC-10.2/SELC-11.2 forbid. Reading a null
    dimension throws. Filling them in later is a change to this map alone, with no application code
    change.
  - **Application side:** `TenantDataIsolationRegistry` (`libs/selfcare-sdk-security`) parses the map
    from one env var (`SELFCARE_TENANT_DATA_ISOLATION` → `selfcare.tenant.data-isolation`) and is
    injectable via `TenantDataIsolationRegistryProducer`. Every lookup is fail-closed: unknown tenant,
    tenant missing from the registry, `null` tenant, or undecided dimension all raise
    `UnresolvedTenantMappingException` instead of returning a fallback (SELC-8.4, SELC-9.4, SELC-10.2,
    SELC-11.2). Lookups take the tenant already validated upstream (`TenantContext`); the registry never
    derives one itself (SELC-8.5, SELC-9.5). Unknown JSON properties are ignored so a new dimension can
    be rolled out to services one at a time; a malformed payload or an unknown tenant key fails at
    startup, since that means the registry and the `TenantId` enum disagree.
  - **Delivery:** `container_app_microservice` takes `tenant_data_isolation_json` and injects the env
    var, so a consuming stack adds one line —
    `tenant_data_isolation_json = module.local.config.tenant_data_isolation_json` — and never a literal
    map. Deliberately **not** yet added to the ~40 existing app stacks: injecting it into services that
    do not read it yet would roll every container app revision for no behavioural gain. Sub-tasks 2–8
    add the line to the stacks they touch.
  - **Not covered:** the Spring Boot apps (`institution-ms`, `user-group-ms`, `external-api`,
    `dashboard-bff`, `registry-proxy`, `onboarding-bff`) do not depend on `selfcare-sdk-security` and
    mirror its tenant classes instead. The mapping *data* stays single-sourced (same env var, same
    Terraform map), but a Spring-side reader is still to be written by the first sub-task that needs one.

### 10. Data migration / backfill for pre-existing single-tenant data
- **Maps to:** Open Question (migration/backfill approach)
- **Description:** For microservices moving to the discriminator-field model, backfill the `tenantId` field on
  existing documents. For microservices moving to per-tenant databases/storage accounts, plan and execute the
  one-time data move.
- **Acceptance criteria:** No pre-existing data is orphaned or misattributed after migration; migration is
  auditable and reversible where feasible.
- **Depends on:** Sub-tasks 1–6.
- **Status: tooling done, execution pending — it still gates sub-task 2.** Every discriminator-model read
  still carries the `or tenantId is null` migration branch, which keeps pre-existing untagged documents
  readable by *both* tenants. That branch cannot be dropped, and the Cosmos DB isolation cannot be called
  enforcement, until the backfill has run in each environment. `scripts/backfill_tenant_id.py` (see
  `scripts/README.md`) performs it across the eight tenant-scoped databases, taking the tenant as an explicit
  argument because it is the connection string — one Cosmos account per tenant — that decides which tenant's
  data is being tagged. It only ever writes to documents that have no `tenantId`, so it cannot reassign an
  already-attributed document and is safe to re-run or interrupt; it is dry-run unless `--apply` is passed.
  Its `--verify` mode is the gate for sub-task 2: it must exit `0` for both tenants in an environment before
  that environment's reads can be made strict. The product catalogue database is deliberately excluded, as
  product definitions are global rather than tenant-owned. No storage/database move is needed, since no
  service selected the per-tenant database or per-tenant account model (sub-tasks 3, 6).

### 11. Security review and audit logging for data-layer tenant enforcement
- **Maps to:** `SECURITY.md` (all sections)
- **Description:** Add logging for fail-closed rejections (unresolved tenant at Cosmos DB, Storage, vault, or
  email layers); verify no PII/secret leakage in logs; confirm Key Vault-only credential handling across all
  new tenant-mapping configuration.
- **Acceptance criteria:** Audit trail exists for every fail-closed rejection; security review sign-off
  obtained before epic closure.
- **Depends on:** Sub-tasks 2, 3, 5, 6, 7, 8.
- **Status: partial, sign-off not requested (correctly — its dependencies are not all done).** Rejections are
  logged at both layers: `TenantValidationFilter` logs every mismatch, missing/duplicated header and the
  hub-spid-login default at the boundary, and `TenantDataIsolationRegistry` now logs each fail-closed refusal
  at WARN before raising the typed exception. Logging inside the registry rather than at its call sites is
  deliberate — a caller that recovers from the exception would otherwise silence a tenant-isolation failure.
  The lines carry only the tenant name and the missing dimension, no PII and no secret value. Credential
  handling for the new tenant-mapping configuration is Key Vault-only by construction: the sub-task 9 registry
  stores secret *names*, and the delivered env var contains no secret value. Still to do: the same treatment
  for the vault/storage/email paths once sub-tasks 5, 7 and 8 create them, a log review over those paths, and
  the security sign-off itself.

---

## Open blockers to resolve before/at epic kickoff
- ~~Per-microservice Cosmos DB isolation model assignment (SELC-8.6).~~ **Resolved (sub-task 1):** discriminator
  field for all 15 Mongo-using services.
- ~~Per-microservice Azure Storage isolation model assignment (SELC-9.6).~~ **Resolved (sub-task 4)** except
  `dashboard-bff` (institution logos), still undecided.
- Personal data vault provider/API contract and full integrating-service list (SELC-10.3).
- Full list of microservices sending tenant-facing email beyond `institution-send-mail-scheduler` (SELC-11.3).
- ~~Shared source of truth for all tenant mappings (Cosmos DB, Storage, vault, email) — same registry as Step 0
  or separate.~~ **Resolved (sub-task 9):** same registry as Step 0, `local.tenant_data_isolation` in
  `infra/resources/_modules/local-env/locals.tf`, read by `TenantDataIsolationRegistry`. The vault and email
  dimensions are declared but left `null` until the two blockers above are closed.
- Ownership of the blob container definitions (found while scoping sub-task 5): the per-tenant containers
  cannot be created from this repository, which only reads the storage accounts via `data` blocks. Whoever owns
  them must provision the per-tenant containers before sub-task 5 can ship.
- A Spring-side reader for the tenant registry: the six Spring Boot apps mirror the tenant classes instead of
  depending on `selfcare-sdk-security`, so they can consume the same env var but have no parser yet.
- ~~Migration/backfill approach for pre-existing single-tenant data.~~ **Resolved (sub-task 10):** in-place
  discriminator backfill via `Step_1/scripts/backfill_tenant_id.py`, run per tenant against that tenant's own
  Cosmos account. Execution in each environment is still outstanding and blocks sub-task 2.
- Regulatory/data-residency constraints that may force a specific isolation model per tenant.
