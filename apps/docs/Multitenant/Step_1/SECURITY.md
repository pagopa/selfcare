# Security Rules — Multitenant Data Isolation (Step 1)

Source inputs: `apps/docs/Multitenant/Step_1/REQUIREMENTS.md`, `apps/docs/Multitenant/Step_1/ARCHITECTURE.md`.

> No local prompt library (`Code Quality/`, `Web and API Security/`, etc.) was found anywhere in this
> repository or environment (re-checked for this step). Every area below therefore falls back to public OWASP
> standards (OWASP Top 10 Proactive Controls, OWASP Cheat Sheet Series, OWASP API Security Top 10), as
> instructed when no matching prompt exists. Where ARCHITECTURE.md points to concrete repository paths
> (`/infra/core/_modules/cosmos_db`, `/infra/resources/document-ms`, etc.), those paths are cited as the
> architecture grounding, not as a prompt source.

## Required Security Inputs

- Per-microservice Cosmos DB isolation model (discriminator field vs. database-per-tenant): `TO BE DECIDED` (SELC-8.6) — access-control rules below must be applied to whichever model each service ends up using; no default model is assumed.
- Per-microservice Azure Storage isolation model (shared account/per-tenant container vs. per-tenant account): `TO BE DECIDED` (SELC-9.6) — same caveat as above.
- Personal data vault provider, API contract, and full list of integrating microservices: `TO BE DECIDED` (SELC-10.3) — only `onboarding-ms` and `auth` are confirmed callers today.
- Full list of microservices sending tenant-facing email beyond `institution-send-mail-scheduler`: `TO BE DECIDED` (SELC-11.3).
- Source of truth for tenant → Cosmos DB database / storage account-container / vault-tenant / email-domain mappings, and whether it is shared with Step 0's host/path registry: `TO BE DECIDED` (Open Question).
- Migration/backfill approach for pre-existing single-tenant data (discriminator backfill or per-tenant move): `TO BE DECIDED` (Open Question).
- Regulatory/data-residency constraints forcing a specific isolation model for a given tenant: `TO BE DECIDED` (REQUIREMENTS.md "Regulatory or privacy constraints").
- Scale expectations for this step: `TO BE DECIDED` — no throughput/latency figures given; Step 0's APIM-level throttling is upstream and not re-derived here.

## Provisional Security Rules

### Cosmos DB tenant isolation (SELC-8)
- Every Cosmos DB read/write/update/delete path MUST include the already-validated tenant (from Step 0) in its filter or connection selection — never re-derive tenant at the data layer (SELC-8.5). A repository method missing this filter is a cross-tenant data-leak defect, not a style issue (OWASP Top 10 Proactive Controls — C1: Access Control, no dedicated Cosmos DB prompt directory found in this repository).
- Discriminator-field model: treat every new or modified Panache/Mongo query as untrusted-by-default until it demonstrably includes the `tenantId` filter; add this to code review checklists, since a missed filter is silent and does not fail loudly at runtime.
- Database-per-tenant model: the `TenantResolver` MUST fail closed (reject) when it cannot map the resolved tenant to a known Cosmos DB database — never fall back to a default/shared database (SELC-8.4).
- Do not allow a microservice to mix both isolation models for the same data entity (SELC-8.2) — this creates two divergent code paths that are hard to audit consistently.
- Cosmos DB connection strings/keys MUST stay in Key Vault (grounded in `infra/core/_modules/key_vault`, consistent with Step 0 `SECURITY.md` secret-handling rules); a database-per-tenant model must not multiply plaintext connection strings in app config.

### Azure Storage tenant isolation (SELC-9)
- Every blob/file operation MUST resolve to the container or storage account belonging to the already-validated tenant; on failure to resolve, reject the operation — never fall back to a default container/account (SELC-9.3, SELC-9.4). No dedicated Azure Storage prompt directory found; fallback: OWASP API Security Top 10 — API1:2023 Broken Object Level Authorization, applied to blob/container access instead of API objects.
- Shared-account/per-tenant-container model: treat container name/selection as security-relevant input — derive it only from the validated tenant, never from client-supplied parameters (path, filename, or header), to prevent path/container-confusion across tenants.
- Per-tenant storage account model: apply the same fail-closed rule as the Cosmos DB `TenantResolver` — unresolved tenant must reject the request, not default to any existing account.
- Reuse existing least-privilege access patterns already used for `document-ms` storage accounts (`data.azurerm_storage_account.documents_storage`, `user_attachments_storage` in `infra/resources/document-ms/*/main.tf`) — do not widen SAS/managed-identity scope to cover multiple tenants' containers in one grant when adding tenant dimensioning.
- Storage account keys/SAS tokens MUST remain in Key Vault-backed configuration, never inlined in Terraform or app config (OWASP Secrets Management Cheat Sheet fallback; consistent with Step 0 `SECURITY.md`).

### Personal data vault
- Any call to the personal data vault MUST use the tenant already validated upstream to select the vault instance/tenant; on an unmapped tenant, reject the call rather than default to another tenant's vault (SELC-10.2). No dedicated "personal data vault" prompt directory found; fallback: OWASP Top 10 Proactive Controls — C1 Access Control, C8 Protect Data Everywhere (PII handling).
- Treat personal data vault responses as PII: apply the same never-log-PII discipline already required for JWTs/tenant claims in Step 0 `SECURITY.md` — do not log vault payloads, only correlation identifiers.
- Until the vault provider/API contract is confirmed (SELC-10.3), do not assume its transport is already secured — verify TLS and credential handling explicitly when the integration is designed, rather than inheriting an assumption from existing `onboarding-ms`/`auth` usage.

### Tenant-aware outbound email
- Outbound email MUST use the sender domain mapped to the already-validated tenant; if tenant cannot be resolved for the trigger, block the send and surface an explicit error — never default to another tenant's domain (SELC-11.2). No dedicated email-security prompt directory found; fallback: OWASP Cheat Sheet Series — general secure-by-default guidance on avoiding unauthenticated/ambiguous sender identity.
- Ensure any new tenant-facing email flow does not become an open relay across tenants: the trigger's tenant, the template used, and the sender domain must all agree before send — mismatches should hard-fail, mirroring the header/claim consistency rule from Step 0.

### Shared cross-cutting rule (all four areas)
- None of these components (Cosmos DB, Storage, vault, email) may re-derive tenant identity independently; all must consume the single tenant value already validated by Step 0's header/JWT-claim reconciliation (SELC-8.5, SELC-9.5) — reduces the risk of a component drifting from the canonical tenant-resolution outcome and creating an inconsistent trust boundary.
- Any new mapping table (tenant → database/account/vault-tenant/email-domain) MUST be stored as non-secret routing/config data separately from any actual credentials, following the same Key Vault-reference pattern already used for Step 0's APIM Named Values (Step 0 `SECURITY.md` — Secret handling).

## Selected Prompts

- `Cosmos DB / data-layer access control -> no dedicated prompt directory found; fallback: OWASP Top 10 Proactive Controls — C1 Access Control (+ Quarkus/Cosmos DB Mongo API multitenancy best practices)`
- `Azure Storage tenant isolation -> no dedicated prompt directory found; fallback: OWASP API Security Top 10 — API1:2023 Broken Object Level Authorization, applied to blob/container access`
- `Backend framework -> Quarkus (no dedicated prompt directory found; apply Quarkus Security Guide + OWASP fallback, consistent with Step 0)`
- `Personal data vault / PII handling -> no dedicated prompt directory found; fallback: OWASP Top 10 Proactive Controls — C8 Protect Data Everywhere`
- `Tenant-aware outbound email -> no dedicated prompt directory found; fallback: OWASP Cheat Sheet Series — general secure-by-default sender identity guidance`
- `Secret management -> /infra/core/_modules/key_vault (fallback guidance: OWASP Secrets Management Cheat Sheet, consistent with Step 0 SECURITY.md)`
- `Deployment / infra (Cosmos DB, Storage provisioning) -> /infra/core/_modules/cosmos_db, /infra/resources/_modules/cosmosdb_database, /infra/resources/_modules/cosmosdb_collection, /infra/resources/document-ms (fallback guidance: OWASP Cheat Sheet Series — Docker/Container Security, CI/CD, consistent with Step 0)`
- `Authorization model (per-microservice isolation model choice) -> UNRESOLVED / TO BE DECIDED — SELC-8.6 and SELC-9.6 leave the model per service undecided; no RBAC/ABAC/ReBAC prompt directory or library found`
- `Code quality -> OWASP Top 10 Proactive Controls (fallback — no "Code Quality" prompt directory found in repository, consistent with Step 0)`
