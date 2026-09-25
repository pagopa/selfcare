# Runtime Tenant Resource Routing and Strict Isolation (Step 2)

This step builds on `apps/docs/Multitenant/Step_1`. Step 1 introduced the tenant data-isolation model,
the shared tenant registry, tenant propagation, the Cosmos DB discriminator rollout, and the migration
tooling. Step 2 completes the runtime routing to shared or dedicated resources and defines the gates that
must be satisfied before a single backend deployment can safely serve both tenants.

The current tenants are:

- `AR`: `selfcare.pagopa.it`
- `PNPG`: `imprese.notifichedigitali.it`

## Inherited Baseline from Step 1

The following decisions are already established and are not reopened by this step:

- Incoming requests use the tenant already validated by the Step 0 header/JWT reconciliation and exposed
  through `TenantContext`; data-access components MUST NOT derive the tenant again from raw request data.
- Tenant-scoped Cosmos DB entities use a `tenantId` discriminator. `product` uses
  a tenant path variable and persists `tenantId` on product and contract-template
  documents; the IAM role catalogue remains global because it contains shared
  platform configuration rather than tenant-owned data.
- Product-driven dedicated database routing in `onboarding-ms` is an independent isolation dimension. A
  dedicated product database may still contain data from multiple tenants and therefore MUST retain the
  `tenantId` discriminator.
- Tenant resource metadata is centrally declared by `local.tenant_data_isolation` in
  `infra/resources/_modules/local-env/locals.tf` and delivered to applications through
  `SELFCARE_TENANT_DATA_ISOLATION`.
- Cosmos DB Mongo API access does not use Managed Identity. Connection strings and credentials remain in
  Azure Key Vault; the tenant registry contains only the environment-variable name used to receive each
  secret-backed value. Terraform owns the mapping from Key Vault secret to application environment variable.
- `TenantDataIsolationRegistry` lookups are fail-closed: an unknown tenant, a missing mapping, or an undecided
  resource dimension MUST raise an explicit error and MUST NOT return a default tenant's resource.
- Existing Cosmos DB records are migrated with
  `apps/docs/Multitenant/Step_1/scripts/backfill_tenant_id.py`. Until migration is verified, the temporary
  `tenantId = currentTenant OR tenantId IS NULL` read predicate remains controlled by
  `SELFCARE_TENANT_STRICT_DATA_ISOLATION`.
- Azure Storage topology is configured per tenant and per logical storage purpose. A tenant may use multiple
  storage accounts, and the same physical account may be reused by multiple logical purposes when explicitly
  configured. `onboarding-ms` initially requires the `products` binding; additional bindings can be added
  without changing the tenant-registry schema.
- Personal Data Vault identifiers and outbound email sender domains remain intentionally unset until their
  authoritative values and ownership are agreed.

## Required Requirement Inputs

- **Project purpose:** Complete tenant-aware runtime resource selection so a shared deployment routes every
  operation to the correct Cosmos DB data, Azure Storage location, Personal Data Vault tenant, and outbound
  email identity.
- **Primary actors:** HTTP-facing backend microservices, service-to-service clients, CDC/event consumers,
  scheduled jobs, Azure Functions, APIM, Terraform deployment pipelines, and operators executing data
  migration and cutover.
- **Tenant source:** A validated `TenantContext` for request-driven work; a persisted or signed tenant identity
  for asynchronous work where no request context exists.
- **Resource source of truth:** `local.tenant_data_isolation`, with non-secret routing metadata and
  environment-variable references distributed as application configuration. Terraform maps Key Vault-backed
  secrets to those variables. Cosmos DB Mongo and Storage connection strings MUST NOT be stored in the
  registry or Terraform state as plaintext.
- **Resource topology:** A resource may be shared or dedicated. The choice MUST be explicit per resource and
  service, and routing MUST preserve tenant isolation in either topology.
- **Migration model:** Environment-by-environment migration with explicit verification gates, parallel
  availability of legacy resources where required, and rollback before legacy decommissioning.
- **Privacy constraint:** Cross-tenant reads, writes, object access, vault access, or sender identity selection
  are security defects. Regulatory or data-residency constraints that require physical separation remain to
  be confirmed.

## Resource Routing Matrix

| Resource | Step 2 routing model | Scope |
|---|---|---|
| Cosmos DB tenant-owned documents | Shared database after migration, isolated by strict `tenantId` discriminator | All tenant-scoped Mongo entities |
| Cosmos DB product catalogue | Shared or dedicated database selected by tenant, isolated by strict `tenantId` discriminator | `product` product and contract-template documents |
| Cosmos DB IAM roles | Shared and intentionally unscoped | Global platform configuration |
| Cosmos DB dedicated product data | Database selected from product configuration, plus tenant discriminator | `onboarding-ms` product-driven routing |
| Cosmos DB Mongo credentials | Connection string resolved from a tenant/resource-specific Azure Key Vault secret reference; no Managed Identity | Every Mongo client configuration |
| Azure Storage logical bindings | Account/container selected by validated tenant and logical storage key | `onboarding-ms` products initially; contracts, attachments, templates, archives, and other purposes as adopted |
| Azure Storage tenant-owned documents | Shared or dedicated account with tenant-specific container/path | `document-ms` contracts and attachments |
| Azure Storage shared assets | Explicitly configured shared account/container; never an implicit fallback | Product, mail, and contract templates classified as global |
| Personal Data Vault | Tenant-specific instance or logical tenant | Known callers: `auth`, `onboarding-ms`; final provider inventory required |
| Outbound email | Tenant-specific sender domain and tenant-compatible credentials | At least `institution-send-mail-scheduler`; complete sender inventory required |

## Functional Requirements

### SELC-12: Canonical Runtime Tenant Resolution

- **SELC-12.1:** Request-driven resource selection MUST use only the tenant stored in the validated
  `TenantContext`. Raw `X-Tenant-Id`, JWT claims, hostnames, paths, or request parameters MUST NOT be parsed
  again by repositories, storage clients, vault clients, or email senders.
- **SELC-12.2:** Asynchronous producers MUST persist the validated `tenantId` in every message, activity
  payload, change event, or scheduled-work record required by a downstream consumer.
- **SELC-12.3:** Asynchronous consumers and schedulers MUST resolve the tenant from that persisted identity or
  from a tenant-bound machine credential. They MUST NOT process tenant-owned data through an unscoped query
  merely because no HTTP request context exists.
- **SELC-12.4:** Service-to-service calls MUST propagate a corroborated tenant identity. Calls through APIM
  MUST use a subscription-to-tenant mapping; direct private calls MUST propagate both the tenant header and a
  compatible JWT claim where authentication is required.
- **SELC-12.5:** Unknown, missing, duplicated, inconsistent, or unmapped tenant identities MUST fail closed
  before access to any downstream resource.

### SELC-13: Strict Cosmos DB Isolation and Migration

- **SELC-13.1:** Every read, insert, update, delete, aggregation, lookup, and uniqueness constraint involving a
  tenant-owned entity MUST include `tenantId` or be enforced through a data-access chokepoint that adds it.
- **SELC-13.2:** Existing documents MUST be backfilled separately for `AR` and `PNPG` by using each tenant's
  current Cosmos account as the ownership boundary. The migration MUST never infer ownership from document
  contents or overwrite a non-null `tenantId`.
- **SELC-13.3:** For each environment, strict isolation MUST NOT be enabled until the Step 1 backfill
  `--verify` command succeeds for both tenants and all expected databases and collections.
- **SELC-13.4:** After verification, `strict_tenant_data_isolation` MUST be enabled consistently for every
  service in that environment. Partial strictness, where some services still accept untagged records, MUST
  block database consolidation.
- **SELC-13.5:** Once all environments operate in strict mode, the temporary feature flag and every
  `tenantId IS NULL` compatibility branch MUST be removed so isolation is enforced by construction.
- **SELC-13.6:** When AR and PNPG databases are merged, unique indexes for tenant-owned identifiers MUST be
  reviewed and changed from global uniqueness to tenant-qualified uniqueness where the same logical value may
  exist in both tenants, for example `(tenantId, email)` or `(tenantId, externalId)`.
- **SELC-13.7:** Data import MUST be auditable, deterministic, restartable where feasible, and MUST reject
  conflicting records instead of silently selecting one tenant's version.
- **SELC-13.8:** Product-driven database selection MUST fail closed for unknown products, lookup failures, or
  `DEDICATED` products without a database name. Endpoints that only know an `onboardingId` MUST be able to
  resolve the correct product database before any product is configured as `DEDICATED`.
- **SELC-13.9:** Cosmos DB Mongo API clients MUST authenticate through connection strings or equivalent Mongo
  credentials stored in Azure Key Vault. The design MUST NOT depend on Managed Identity for Mongo
  authentication.
- **SELC-13.10:** Each Cosmos routing entry MUST identify the target account/database and the Key Vault secret
  name or secret reference containing its connection string. The routing registry MUST NOT contain the
  connection string value.
- **SELC-13.11:** Terraform MUST expose the required Key Vault secrets to the shared deployment through
  distinct secret-backed configuration keys. It MUST support at least one connection configuration per
  tenant/resource where separate Cosmos accounts or credentials coexist during migration or for a dedicated
  resource.
- **SELC-13.12:** Applications MUST initialize Mongo clients from the injected Key Vault-backed configuration
  and select the correct client/database at runtime using the validated tenant and, where applicable, product
  routing. They MUST NOT query Key Vault on every data operation.
- **SELC-13.13:** A missing, inaccessible, empty, or malformed Cosmos DB secret MUST fail deployment startup
  or reject use of the affected route. It MUST NOT fall back to another tenant's connection string or to a
  generic default client.
- **SELC-13.14:** Key Vault secret rotation MUST be possible without changing the tenant registry schema or
  application code. The deployment procedure MUST define how applications reload or restart after a secret
  version changes.

### SELC-14: Azure Storage Routing

- **SELC-14.1:** Every tenant-aware storage operation MUST resolve a storage binding from the validated tenant
  and an application-defined logical storage key. `onboarding-ms` MUST initially define the `products`
  logical key and MUST NOT read its product catalogue from one global account/container setting.
- **SELC-14.2:** Each tenant entry in `tenant.registry.json` MUST support a `storages` object containing zero or
  more bindings keyed by a stable logical name such as `products`, `contracts`, `attachments`, `templates`, or
  `archives`. The structure MUST be a map, not an ordered array, so lookup is deterministic and duplicate
  logical keys are rejected by JSON parsing/configuration validation.
- **SELC-14.3:** Each storage binding MUST identify the Azure Storage account and container, MAY define a
  trusted path prefix, and MUST explicitly select its authentication mode. Connection-string authentication
  MUST reference a secret-backed environment variable; Managed Identity authentication MUST reference the
  account name and MAY reference a user-assigned managed identity client-ID environment variable.
- **SELC-14.4:** A tenant MAY map different logical storage keys to different accounts, containers, credentials,
  or identity assignments. Multiple logical keys MAY intentionally point to the same physical account or
  container, but that reuse MUST be explicit in every binding.
- **SELC-14.5:** Shared templates and other global assets MUST remain in explicitly classified shared
  containers. Tenant-owned and shared operations MUST be distinguished by the application call site or a
  typed API, never by parsing a client-supplied blob path.
- **SELC-14.6:** Storage clients MUST be initialized once per distinct credential/account configuration and
  selected at operation time so interleaved requests for `AR` and
  `PNPG` in the same process cannot reuse a client, container, or path belonging to the previous request.
- **SELC-14.7:** Resolving a storage binding MUST use only `TenantContext` and a code-owned logical key. Account
  names, container names, authentication modes, environment-variable names, and path prefixes MUST NOT be
  accepted from an HTTP request, JWT claim, event payload, or blob name.
- **SELC-14.8:** A missing tenant, unknown logical key, incomplete binding, missing credential, missing
  container, or unavailable account MUST reject the affected operation. Falling back to another logical key,
  a legacy application property, a base container, or another tenant's account is forbidden.
- **SELC-14.9:** Registry validation MUST verify all mandatory storage bindings for every supported tenant at
  startup. Optional bindings MAY be validated on first use, but an unconfigured optional binding MUST still
  fail closed.
- **SELC-14.10:** Required tenant containers and access grants MUST be provisioned before application routing is
  enabled. Ownership of container provisioning outside this repository MUST be identified and included in the
  rollout plan.
- **SELC-14.11:** `dashboard-bff` institution-logo storage MUST be classified as tenant-owned or global before
  its deployment is consolidated. No implicit classification is allowed.
- **SELC-14.12:** CDC archive writers MUST preserve the upstream `tenantId` in object metadata, path, or payload
  according to their established contract; they MUST NOT invent a tenant when the source event is unscoped.

### SELC-15: Personal Data Vault Routing

- **SELC-15.1:** The Personal Data Vault provider, API contract, tenant identifiers, credential model, and
  complete caller inventory MUST be documented before a shared deployment enables vault access for both
  tenants.
- **SELC-15.2:** Every vault operation MUST select the instance or logical vault tenant mapped to the validated
  runtime tenant through the central registry.
- **SELC-15.3:** Missing or null `personal_data_vault_tenant` configuration MUST block the operation; it MUST
  NOT resolve to a shared/default vault tenant.
- **SELC-15.4:** Vault payloads and credentials MUST NOT be logged. Audit records may contain only the tenant,
  operation outcome, and non-PII correlation identifiers.

### SELC-16: Tenant-Aware Outbound Email

- **SELC-16.1:** The complete inventory of tenant-facing email producers MUST be documented. It MUST include
  direct senders and services that enqueue `MailNotification` records.
- **SELC-16.2:** Every notification MUST retain `tenantId` from creation through delivery. The scheduler MUST
  select the sender address/domain and any tenant-specific machine credential from that tenant identity.
- **SELC-16.3:** Missing or null `email_sender_domain`, missing tenant credentials, or a mismatch between the
  notification tenant and sender identity MUST block delivery and produce an explicit retryable or terminal
  failure according to the existing scheduler policy.
- **SELC-16.4:** The scheduler MUST NOT use a single tenant's token or sender identity while processing a
  cross-tenant batch. Credentials MUST be tenant-bound or selected per notification.
- **SELC-16.5:** SMTP/API credentials remain secrets in Azure Key Vault; the registry may contain only
  non-secret sender-domain metadata and secret references.

### SELC-17: Shared and Dedicated Resource Configuration

- **SELC-17.1:** Every tenant-dependent configuration currently represented by separate `-ar` and `-pnpg`
  deployments MUST be classified as global, tenant-specific, or environment-specific before the owning
  deployment is consolidated.
- **SELC-17.2:** Tenant-specific settings MUST use distinct configuration keys or secret references in the
  shared deployment. Two different tenant values MUST NOT be collapsed into one legacy environment variable.
- **SELC-17.3:** A dedicated resource MUST be selected through the same canonical tenant registry and
  fail-closed rules as a shared resource. Dedicated topology MUST NOT introduce a second tenant registry.
- **SELC-17.4:** The registry MUST contain resource identifiers and environment-variable references, not Key
  Vault secret names, connection strings, access keys, SAS tokens, private keys, or SMTP passwords.
- **SELC-17.5:** Cosmos DB, Storage, vault, and email secret values MUST be supplied through Azure Key Vault
  references or the platform's equivalent secret injection mechanism. Configuration that embeds those values
  directly in Terraform variables, environment files, or the tenant registry is forbidden.
- **SELC-17.6:** Applications MUST validate mandatory resource mappings at startup where all mappings are
  expected to be complete; intentionally deferred dimensions such as vault or email MUST fail on first use
  until configured.

### SELC-18: Consolidation and Cutover Gates

- **SELC-18.1:** A microservice deployment MUST NOT be consolidated until its inbound tenant validation,
  outbound tenant propagation, data isolation, tenant-specific configuration, and downstream network reachability
  are verified for both tenants.
- **SELC-18.2:** Consolidation order MUST follow the internal service call graph: dependencies and leaf
  services are migrated before their callers; BFFs, gateways, and orchestration entry points are migrated last.
- **SELC-18.3:** Stateful services MUST complete strict discriminator enforcement, index migration, data
  reconciliation, and data import before traffic is routed to the shared database.
- **SELC-18.4:** Stateless services with tenant-specific flags, destinations, or event behavior MUST resolve
  those differences explicitly; they MUST NOT be treated as configuration-only when behavior differs between
  the legacy deployments.
- **SELC-18.5:** Terraform state migration MUST guarantee that an Azure resource is owned by exactly one state
  before cutover. Importing a resource without removing it from the legacy state is not sufficient.
- **SELC-18.6:** Legacy stacks and resources MUST remain available for rollback until tenant-isolation,
  authentication, functional, and observability checks pass in the target environment.
- **SELC-18.7:** Legacy resources MUST be decommissioned only after production validation confirms that both
  tenants are served by the shared deployment without cross-tenant access or unresolved-resource fallback.

## Non-Functional Requirements

- **Security:** Every tenant-routing failure is fail-closed and observable. Logs MUST NOT expose tokens,
  connection strings, credentials, document contents, vault payloads, or other PII.
- **Consistency:** The same tenant identifier MUST select a coherent set of Cosmos DB, Storage, vault, and
  email resources. Partial tenant provisioning MUST block only the affected operation and surface the missing
  dimension explicitly.
- **Availability:** Resource selection MUST not perform a Key Vault lookup or create a new SDK client on every
  request. Cosmos DB connection strings and other secret-backed configuration SHOULD be injected from Key
  Vault during deployment/startup; clients SHOULD be initialized once and selected at runtime.
- **Testability:** Each resource resolver MUST have tests for both known tenants, unknown/missing tenants,
  cross-tenant access attempts, and interleaved tenant requests. Data-access tests SHOULD use a real or
  containerized datastore when mocks cannot prove query isolation.
- **Auditability:** Migration, strict-mode activation, fail-closed rejections, and cutover decisions MUST leave
  an environment-specific audit trail.

## Acceptance Criteria

- Both `AR` and `PNPG` requests reach the same eligible backend deployment and select only their own
  tenant-scoped resources.
- Cosmos DB verification succeeds for both tenants before strict mode and database merge; no production query
  accepts untagged tenant-owned documents after migration completion.
- Every Cosmos DB Mongo client obtains its connection configuration from a Key Vault-backed secret reference;
  no Managed Identity dependency or plaintext connection string is introduced.
- `document-ms` routes tenant-owned blobs to tenant-specific locations while shared templates remain global.
- `onboarding-ms` resolves the `products` storage binding independently for `AR` and `PNPG`; a configuration
  test demonstrates that the two tenants may use different accounts and that one tenant may expose more than
  one logical storage binding.
- Adding a new logical storage purpose requires registry and infrastructure configuration only; it does not
  require a new top-level tenant-registry field or a positional array contract.
- Personal Data Vault and email operations remain disabled for a tenant until explicit mappings and
  credentials exist; no default tenant is used.
- Service-to-service, CDC, scheduled, and Azure Functions workflows preserve tenant identity without relying
  on an HTTP request context.
- Terraform and application configuration can represent both tenants simultaneously without secret-name or
  environment-variable collisions.
- A tested rollback path exists until each environment's shared deployment is accepted and its legacy
  resources are formally decommissioned.

## Open Decisions and Blockers

- Define the Personal Data Vault provider, API contract, tenant identifiers, credentials, and final caller
  inventory.
- Define the authoritative sender domain/address and machine credential for each tenant, and complete the
  email-producer inventory.
- Decide whether `dashboard-bff` institution logos are tenant-owned or global.
- Confirm whether regulatory or data-residency constraints require physical resource separation for either
  tenant.
- Define the initial authoritative storage-binding inventory for `onboarding-ms` beyond `products`, including
  which bindings are mandatory at startup and which are optional until first use.
- Provision every configured tenant/logical-key container and its least-privilege access grants in the system
  that owns it.
- Resolve `onboarding-ms` lookups that have only an `onboardingId` before enabling any product with a
  `DEDICATED` database.
- Complete tenant-qualified identity/index migrations, including `userInfo`, institution `externalId`, and
  other identifiers that are only globally unique because the legacy databases are separate.
- Replace tenant-specific deployment-token fallbacks in schedulers and Azure Functions with tenant-bearing
  payloads or credentials before consolidating those workloads.
- Resolve APIM-routed service-to-service clients that currently depend on subscription keys, either by
  configuring subscription-to-tenant mappings or by moving them to authenticated private service URLs.
- Define rate-limit thresholds, replica bounds, SLOs, RTO, and RPO for the combined tenant workload.

## Out of Scope

- Replacing the Step 0 tenant identification and JWT/header reconciliation mechanism.
- Introducing a new tenant identifier format.
- Fine-grained authorization inside a tenant beyond the existing application roles and permissions.
- Removing tenant-specific product catalogue queries once product has adopted
  strict `tenantId` persistence and migration is complete.
