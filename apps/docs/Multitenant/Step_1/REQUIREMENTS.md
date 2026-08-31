# Multitenant Data Isolation for Selfcare Microservices (Step 1)

Builds on `apps/docs/Multitenant/Step_0` (tenant identification via `X-Tenant-Id` header and JWT claim,
consistent tenant-resolution strategy across OneIdentity/`auth` and `hub-spid-login`). This step covers what
happens **after** the tenant and the token have been identified and validated: how each microservice isolates
tenant data in Azure Cosmos DB, Azure Storage, the personal data vault, and outbound email.

## Required Requirement Inputs

- Project purpose: Once tenant and token are validated (Step 0), refactor each microservice's data layer so a single shared backend deployment can isolate per-tenant data in Azure Cosmos DB and Azure Storage, select the correct personal data vault tenant, and send email from the correct tenant domain — instead of relying on separate deployments per tenant for data isolation.
- Primary users / actors: Backend microservices under `apps/` that persist data in Cosmos DB and/or Azure Storage, or send email, or access the personal data vault, on behalf of an authenticated tenant request; end users of `selfcare.pagopa.it` and `imprese.notifichedigitali.it` whose data must remain isolated per tenant.
- Core workflows: Per-request tenant-aware data access (Cosmos DB reads/writes), per-request tenant-aware blob/file access (Azure Storage), per-request/per-tenant personal data vault selection, tenant-aware outbound email sending (correct sender domain).
- Business objects / data entities: Cosmos DB documents (and the tenant discriminator field, where used); Cosmos DB databases (where database-per-tenant is used); Azure Storage accounts and containers holding tenant data; personal data vault tenant identifier; outbound email sender domain per tenant.
- External integrations: Azure Cosmos DB (Mongo API, per `infra/core/_modules/cosmos_db`, `infra/resources/_modules/cosmosdb_database`, `infra/resources/_modules/cosmosdb_collection`); Azure Storage (per-app storage accounts under `infra/resources/<app>/*`); personal data vault (tenant-specific instance, provider/mechanism TO BE DECIDED); outbound email/SMTP provider (per-app, e.g. `institution-send-mail-scheduler`).
- Authentication / roles: Relies on tenant identity already established and validated per Step 0 (`X-Tenant-Id` header reconciled with the JWT tenant claim); this step does not change authentication, only how validated tenant identity drives data-layer decisions.
- Regulatory or privacy constraints: TO BE DECIDED — tenant data isolation requirements may be influenced by data residency/privacy rules specific to each tenant (e.g., SPID-related data handling for `imprese.notifichedigitali.it`); exact constraints not yet specified.

## Functional Requirements

### SELC-8: Cosmos DB Tenant Data Isolation
- SELC-8.1: For each microservice that persists data in Cosmos DB, the system MUST isolate tenant data using exactly one of two models: (a) discriminator field — a `tenantId` field present on every document and included in every read/write/query filter, or (b) database-per-tenant — a `TenantResolver` that routes each request to the Cosmos DB database dedicated to the resolved tenant.
- SELC-8.2: The isolation model choice MUST be made and documented per microservice (an inventory of which microservice uses which model); a microservice MUST NOT mix both models for the same data entity.
- SELC-8.3: When the discriminator field model is used, every document read, write, update, and delete operation MUST include the resolved tenant in its filter; an operation without a tenant filter MUST be treated as a defect, not an acceptable omission.
- SELC-8.4: When the database-per-tenant model is used, the `TenantResolver` MUST fail closed — it MUST reject the request rather than fall back to a default database — when the resolved tenant cannot be mapped to a known Cosmos DB database.
- SELC-8.5: In both models, the tenant used for data isolation MUST be the same tenant already validated per Step 0 (SELC-2.3/SELC-3.1 of `Step_0/REQUIREMENTS.md`); it MUST NOT be re-derived independently at the data-access layer.
- SELC-8.6: Which specific microservices require which Cosmos DB isolation model: TO BE DECIDED (requires per-app inventory).

### SELC-9: Azure Storage Tenant Data Isolation
- SELC-9.1: For each microservice that persists data in Azure Storage, the system MUST isolate tenant data using exactly one of two models: (a) shared storage account with a different container per tenant, or (b) a dedicated storage account per tenant, selected via a tenant-resolution mechanism analogous to the Cosmos DB `TenantResolver`, failing closed on an unresolved tenant.
- SELC-9.2: The isolation model choice MUST be made and documented per microservice (an inventory of which microservice uses which model); a microservice MUST NOT mix both models for the same data entity.
- SELC-9.3: When the shared-account/per-tenant-container model is used, every blob/file read, write, and delete operation MUST target the container belonging to the resolved tenant; an operation that cannot determine the correct container MUST be rejected, not defaulted to a fallback container.
- SELC-9.4: When the per-tenant storage account model is used, resolution to the wrong or an unknown tenant's storage account MUST cause the request to be rejected, not silently redirected to a default account.
- SELC-9.5: In both models, the tenant used for storage isolation MUST be the same tenant already validated per Step 0; it MUST NOT be re-derived independently at the storage-access layer.
- SELC-9.6: Which specific microservices require which Azure Storage isolation model: TO BE DECIDED (requires per-app inventory; candidates include `document-ms`, `onboarding-ms`, per `infra/resources/document-ms/*`, `infra/resources/onboarding-ms/*`).

### SELC-10: Personal Data Vault Tenant Selection
- SELC-10.1: For each microservice that reads or writes personal data via the personal data vault, the system MUST select the personal data vault instance/tenant corresponding to the resolved and validated tenant of the current request.
- SELC-10.2: If the resolved tenant cannot be mapped to a known personal data vault tenant, the system MUST reject the request rather than default to another tenant's vault.
- SELC-10.3: The mapping between tenant identifier and personal data vault tenant MUST be maintained per microservice that integrates with the vault; which microservices integrate with the personal data vault, and the vault provider/mechanism itself: TO BE DECIDED.

### SELC-11: Tenant-Aware Outbound Email
- SELC-11.1: For each microservice that sends outbound email, the system MUST select the sender email domain corresponding to the resolved and validated tenant of the request or workflow that triggered the email.
- SELC-11.2: If the tenant cannot be resolved for an outbound email trigger, the system MUST NOT send the email using an arbitrary or default tenant's domain; the system MUST reject or hold the send and surface an explicit error.
- SELC-11.3: The mapping between tenant identifier and sender email domain MUST be maintained per microservice that sends email (e.g., `institution-send-mail-scheduler`, per `infra/resources/institution-send-mail-scheduler/*`); which other microservices send tenant-facing email: TO BE DECIDED (requires per-app inventory).

## Open Questions

- Which specific Cosmos DB isolation model (discriminator field vs. database-per-tenant) applies to each microservice — is this decision per-microservice, per-data-entity, or uniform across the platform?
- Which specific Azure Storage isolation model (shared account/per-tenant container vs. per-tenant account) applies to each microservice, and are there existing storage accounts that already need retrofitting rather than greenfield design?
- What is the personal data vault provider and integration mechanism, and which microservices currently integrate with it?
- What is the source of truth for the tenant → Cosmos DB database, tenant → storage account/container, tenant → personal data vault tenant, and tenant → email domain mappings — is this the same source of truth defined for host/path routing in Step 0 (SELC-6.3), or a separate one?
- For microservices using database-per-tenant or per-tenant storage account, what is the process and rollback plan for provisioning a new tenant's database/account (linked to Step 0's tenant onboarding extensibility, SELC-6.2)?
- Are there regulatory/data-residency constraints that mandate a specific isolation model (e.g., database-per-tenant) for certain tenants or certain data categories, rather than leaving the choice purely to engineering convenience?
- What is the expected behavior for data migrated before this refactor — does existing single-tenant data need a one-time backfill of the `tenantId` discriminator field, or a one-time move into per-tenant databases/storage accounts/containers?
- Should the email-domain and personal-data-vault tenant mappings be validated against the same canonical tenant registry as APIM routing (Step 0), to avoid drift between routing, data isolation, and email/vault configuration?
- What is the consistency/failure requirement when a request's resolved tenant is valid for Cosmos DB isolation but not yet provisioned in Azure Storage, the personal data vault, or the email-domain mapping (partial tenant onboarding)?
