# Security Rules — Runtime Tenant Resource Routing and Strict Isolation (Step 2)

Source inputs:

- `apps/docs/Multitenant/Step_2/REQUIREMENTS.md` (primary)
- `apps/docs/Multitenant/Step_2/ARCHITECTURE.md`
- `apps/docs/Multitenant/Step_1/SECURITY.md` (inherited controls)

> No applicable local security prompt library (`PROMPT.md` under Code Quality, Web and API Security,
> framework, or infrastructure prompt directories) exists in this repository. The only repository prompt is
> unrelated to this scope. The rules below therefore use the public OWASP fallbacks listed in
> **Selected Prompts** and are grounded in the concrete repository architecture under `apps/`, `infra/core`,
> and `infra/resources`.

## Required Security Inputs

- Trusted tenants: `AR` and `PNPG`; the backend consumes the tenant already reconciled from
  `X-Tenant-Id` and JWT `tenant_id` by the Step 0 security boundary.
- Tenant authorization model: mandatory tenant scope on every tenant-owned object and resource; fine-grained
  authorization inside a tenant remains outside Step 2.
- Cosmos DB: Mongo API over private networking, with connection strings stored in Azure Key Vault. Managed
  Identity is not used to authenticate the Mongo client.
- Secret delivery: Azure Container Apps resolves Key Vault-backed secrets through its assigned identity and
  exposes them as secret-backed application configuration. The resolved secret value is then used by the
  Mongo client.
- Data isolation: strict `tenantId` discriminator for tenant-owned Mongo data; explicit unscoped paths only
  for approved global catalogues; product-driven database selection is an additional routing dimension.
- Storage isolation: tenant-specific container/path for tenant-owned `document-ms` objects; explicit shared
  locations for global templates.
- Asynchronous trust model: persisted `tenantId` or tenant-bound machine credentials for CDC, schedulers, and
  Azure Functions.
- Personal Data Vault provider, protocol, tenant identifiers, and credential model: `TO BE DECIDED`.
- Email provider, sender domains, and tenant-specific machine credentials: `TO BE DECIDED`.
- `dashboard-bff` institution-logo classification: `TO BE DECIDED`.
- Regulatory/data-residency constraints, rate limits, SLOs, RTO, and RPO: `TO BE DECIDED`.

## Provisional Security Rules

### HTTP and APIM trust boundary

- APIM MUST derive the tenant only from the configured trusted route and MUST overwrite, never forward, a
  client-provided `X-Tenant-Id`. Unknown routes fail with a generic `403` before reaching a backend.
- CORS allowlists MUST contain exact trusted origins. CORS is a browser control, not authentication or
  authorization, and MUST NOT be used as proof of tenant identity.
- Backend services MUST validate JWT signature and the configured algorithm, trusted issuer, audience,
  expiration, and not-before claims before accepting `tenant_id`.
- The backend MUST compare the trusted APIM header with the JWT tenant claim and reject missing, duplicated,
  unknown, or mismatched values. Resource providers consume only `TenantContext`, never raw headers or claims.
- Every non-public endpoint MUST perform authorization locally. APIM checks do not replace service-side
  access control.
- Subscription keys used by machine clients MUST be bound to one tenant and MUST NOT be the sole credential
  for sensitive operations when a signed service identity is available.

### Tenant and object-level authorization

- Treat every tenant-owned document, blob, notification, and vault record as an object requiring
  tenant-level authorization. Possession of an object ID, onboarding ID, path, or filename never grants
  access.
- Route every tenant-owned Mongo operation through a mandatory repository/query-builder enforcement point
  that adds `tenantId`. Do not expose optional unscoped repository variants.
- Global collections and shared assets MUST use explicit, narrowly named access paths. A new entity or
  operation is tenant-owned by default until reviewed and classified as global.
- Cross-tenant reads SHOULD be indistinguishable from missing objects to callers. Cross-tenant writes and
  deletes MUST be rejected and auditable.
- Unknown or incomplete tenant/resource mappings always deny access. Do not recover by selecting the first,
  default, legacy, or shared resource.
- Product-driven database routing MUST complete before data access. The selected database still enforces the
  tenant discriminator.

### Cosmos DB Mongo credentials and routing

- Store every Cosmos DB Mongo connection string only in Azure Key Vault. Do not place connection strings in
  source code, tenant-registry JSON, Terraform variables, outputs, plans, state-visible literals, logs, or
  diagnostic responses.
- Keep the trust mechanisms distinct: the Container App identity retrieves the Key Vault-backed secret;
  Mongo authenticates with the injected connection string. Do not configure or document Managed Identity as
  the Mongo authentication mechanism.
- Use a distinct secret reference and application configuration key for each route that can have different
  credentials. Never collapse AR and PNPG values into one legacy variable during migration.
- Grant the Container App identity access only to the secrets required by that workload. Separate read access
  from secret administration and avoid broad Key Vault `List` permissions where direct `Get` access is
  sufficient.
- Validate mandatory route metadata and secret-backed settings before accepting traffic. Missing, empty,
  malformed, or inaccessible credentials must fail startup or disable only an explicitly optional route.
- Initialize Mongo clients once from secret-backed configuration and select them per operation from the
  validated tenant/product route. Never cache a request's selected tenant in application-global mutable state.
- Define and test secret rotation as an atomic deployment operation. The old credential must remain valid
  until every active revision can use the new version; stale revisions must then be retired.
- Configure connection, server-selection, socket, and operation timeouts. Bound connection pools per replica
  so autoscaling cannot exhaust Cosmos DB connections or resource units.

### Cosmos DB migration and strict-mode safety

- The backfill MUST run against an explicitly named tenant account and MUST refuse to overwrite a non-null
  `tenantId`, process an unknown tenant, or continue after detecting conflicting ownership.
- Verification MUST inspect every expected collection and fail on inaccessible, unexpectedly missing, empty
  coverage, untagged, or mismatched data.
- Strict mode MUST be enabled consistently for all participating services in one environment before database
  consolidation. Mixed strict/permissive readers are not an acceptable security state.
- Tenant-qualified unique indexes MUST be created and validated before importing records whose identifiers
  may overlap across legacy databases.
- Migration tooling and logs MUST report counts and correlation data, not document payloads, connection
  strings, or PII.
- Preserve a tested rollback route until the shared database and strict filters are verified. Rollback MUST
  not reintroduce writes to both old and new stores without an explicit consistency strategy.

### Azure Storage and file handling

- Select the storage account/container from `TenantContext` and the trusted registry before evaluating any
  client-supplied object path. A filename or path MUST NOT influence whether shared or tenant storage is used.
- Use separate typed operations for tenant-owned documents and global templates. Do not classify storage
  scope by path prefixes supplied by a caller.
- Normalize object names, reject traversal/control characters, and enforce business allowlists for extension,
  content type, file signature, and maximum size. Do not trust the uploaded `Content-Type` or original
  filename.
- Generate or strictly normalize stored filenames and prevent overwrite unless the operation explicitly
  authorizes replacement of the same tenant-owned object.
- Authorize downloads, uploads, updates, and deletes independently. A caller permitted to create an object is
  not automatically permitted to retrieve or delete every object in the same container.
- Provision containers and access grants before enabling tenant routing. Missing containers or permissions
  fail closed; the base/shared container is never a fallback.
- Storage credentials, account keys, and SAS tokens remain Key Vault-backed and are never logged. Prefer
  short-lived, least-privilege SAS scopes when SAS is required.

### Outbound calls and SSRF resistance

- Vault, email, webhook, and internal-service base URLs MUST come from trusted configuration, not request
  parameters or tenant-controlled data.
- Allowlist expected schemes, hosts, and ports for outbound destinations; use HTTPS for external services and
  private DNS for internal services where applicable.
- Disable automatic redirect following when it could escape the destination allowlist. Revalidate the target
  after DNS resolution where a caller can influence any URL component.
- Apply bounded connection/read timeouts, response-size limits, and retry budgets. Retries MUST not multiply
  email sends, writes, or other non-idempotent side effects.

### Asynchronous workloads and machine identities

- Producers MUST stamp `tenantId` from the validated context before persistence or publication. Consumers
  MUST reject missing or unknown tenants rather than treating the event as global.
- Treat tenant identity from queues, events, CDC records, and function payloads as untrusted until the
  transport/source is authenticated and the payload schema is validated.
- Make tenant identity immutable across retries, replays, and state transitions. Updates MUST NOT move an
  existing record or work item to another tenant.
- Use tenant-qualified idempotency keys where duplicate identifiers may exist across tenants.
- Cross-tenant batch workers may enumerate tenants, but each item must select credentials and resources from
  its own immutable `tenantId`. Do not retain one item's tenant or client in shared mutable state.
- A machine token or subscription credential that is fixed to one tenant MUST never process another tenant's
  work.

### Personal Data Vault and PII

- Keep Personal Data Vault operations disabled until the provider, API contract, tenant identifiers, and
  credential model are defined and reviewed.
- Select the vault tenant only from `TenantContext` or an authenticated tenant-bearing async payload. Null or
  missing mappings fail closed.
- Minimize requested and retained PII. Never log vault request/response bodies, decoded identifiers, or
  credentials; use non-PII correlation identifiers.
- Provider TLS validation, endpoint allowlisting, timeout/retry behavior, credential rotation, and tenant
  isolation guarantees require a dedicated review once the provider is selected.

### Tenant-aware email

- Bind notification tenant, template scope, sender address/domain, recipient operation, and machine
  credential before delivery. Any mismatch blocks the send.
- Do not use a global/default sender for an unmapped tenant and do not process a cross-tenant batch with one
  tenant's credential.
- Ensure retries are idempotent and bounded so failures cannot produce duplicate delivery or unbounded
  provider cost.
- Validate provider-side authorization for every sender domain before enabling it. SPF, DKIM, DMARC, bounce
  handling, and abuse controls remain `TO BE DECIDED` with the provider.

### Logging and error handling

- Log tenant-validation failures, unmapped resource dimensions, rejected cross-tenant operations, migration
  gates, secret-loading failures, and cutover decisions as structured security events.
- Include only the normalized tenant ID, operation, resource dimension, outcome, reason code, and correlation
  ID. Exclude JWTs, authorization/subscription headers, connection strings, SAS tokens, secret references
  where sensitive, document contents, email bodies, and vault payloads.
- Sanitize untrusted values before logging to prevent log injection. Limit field sizes and avoid logging raw
  filenames, paths, URLs, or exception messages from external systems.
- Return stable RFC 7807-style errors without revealing whether another tenant's object, database, container,
  vault tenant, or sender identity exists.
- Alert on repeated tenant mismatches, unknown-host requests, cross-tenant access attempts, missing routing
  dimensions, backfill conflicts, and abnormal per-tenant resource consumption.

### Deployment, Terraform, and CI/CD

- Protect Terraform state as sensitive data even when secret values are intended to remain in Key Vault.
  Review plans and outputs for accidental secret material before storing build artifacts.
- Before moving an existing resource to the unified stack, disable competing pipelines, back up state, and
  ensure the resource ID is owned by exactly one Terraform state.
- Require reviewed plans and environment approval for changes to tenant mappings, Key Vault references,
  Cosmos indexes, APIM policies, strict-mode flags, routing, or legacy-resource deletion.
- Do not let pull-request or fork-controlled code access production secrets or deployment identities.
  Environment credentials must be short-lived and least privilege.
- Pin and verify container images and deployment artifacts. Do not deploy mutable tags to production.
- Keep legacy resources available until both tenants pass authentication, isolation, functional,
  observability, and rollback checks; decommissioning is a separate approved action.

### Availability and abuse resistance

- Apply tenant-aware rate limits, concurrency limits, request/body size limits, pagination bounds, and
  timeouts at APIM and service boundaries.
- Bound Container App replicas and Mongo connection pools together; scaling out must not exceed Cosmos DB
  connection or RU capacity.
- Bound batch sizes, queue retries, file sizes, and third-party email/vault consumption. Use dead-letter or
  explicit terminal states instead of infinite retry loops.
- Exact limits and service objectives remain `TO BE DECIDED`; production cutover requires measured values
  from the combined workload.

### Security verification

- Add negative tests for missing, unknown, duplicated, and mismatched tenant identities on each ingress type.
- Test every tenant-owned repository operation with same-tenant success and cross-tenant absence/rejection;
  use a real or containerized datastore when mocks cannot verify the generated query.
- Test interleaved AR/PNPG operations in the same process to detect leaked thread-local, request-scoped, or
  cached client state.
- Test missing/wrong Key Vault secret references, secret rotation, wrong-account configuration, and
  unavailable tenant resources without permitting fallback.
- Test blob path traversal, spoofed content types, oversized files, shared-template access, and tenant
  container separation.
- Treat failure of migration verification, index preparation, state ownership, or rollback rehearsal as a
  release blocker.

## Selected Prompts

- `Code quality -> no local prompt directory found; fallback: OWASP Top 10 Proactive Controls — C1 Implement Access Control`
- `API security -> no local Web and API Security prompt directory found; fallback: OWASP REST Security Cheat Sheet + OWASP API Security Top 10 2023 API1 Broken Object Level Authorization and API4 Unrestricted Resource Consumption`
- `Authentication and JWT -> no local authentication prompt directory found; fallback: OWASP REST Security Cheat Sheet (JWT validation and endpoint access control)`
- `CORS -> no local CORS prompt directory found; fallback: OWASP REST Security Cheat Sheet and least-privilege exact-origin allowlisting`
- `Authorization -> no local RBAC/ABAC/ReBAC prompt directory found; fallback: OWASP Top 10 Proactive Controls — C1 Implement Access Control; fine-grained intra-tenant model remains TO BE DECIDED`
- `File and blob handling -> no local file-upload prompt directory found; fallback: OWASP File Upload Cheat Sheet`
- `Outbound request security -> no local SSRF prompt directory found; fallback: OWASP Server-Side Request Forgery Prevention Cheat Sheet`
- `Secret management -> no local prompt directory found; fallback: OWASP Secrets Management Cheat Sheet; architecture grounding: infra/core/_modules/key_vault and infra/resources/_modules/container_app_microservice`
- `Cosmos DB Mongo isolation -> no dedicated prompt directory found; fallback: OWASP C1 Access Control and API1 Broken Object Level Authorization; architecture grounding: infra/core/_modules/cosmos_db`
- `Backend framework -> Quarkus and Spring Boot; no framework-specific prompt directories found; fallback: OWASP REST Security Cheat Sheet`
- `Azure Functions and asynchronous processing -> no local prompt directory found; fallback: OWASP C1 Access Control, Secrets Management, and Logging Cheat Sheets`
- `Logging and error handling -> no local prompt directory found; fallback: OWASP Logging Cheat Sheet`
- `Deployment and CI/CD -> no local Terraform/Azure Container Apps prompt directory found; fallback: OWASP CI/CD Security Cheat Sheet; architecture grounding: infra/core and infra/resources`
- `Personal Data Vault -> UNRESOLVED / TO BE DECIDED; apply OWASP C1 Access Control, Secrets Management, and data-minimization defaults until the provider is selected`
- `Email provider security -> UNRESOLVED / TO BE DECIDED; apply fail-closed sender binding, secret-management, logging, and resource-consumption defaults until the provider is selected`
