# Runtime Tenant Resource Routing and Strict Isolation Architecture (Step 2)

## Required Architecture Inputs

- `Requirements source: REQUIREMENTS.md` — primary source:
  `apps/docs/Multitenant/Step_2/REQUIREMENTS.md`; inherited context:
  `apps/docs/Multitenant/Step_1/REQUIREMENTS.md`.
- `System purpose:` Allow one backend deployment per environment to serve `AR` and `PNPG` while selecting
  the correct shared or dedicated Cosmos DB, Azure Storage, Personal Data Vault, and email resources from a
  validated tenant identity. Complete strict data isolation and the controlled migration from the legacy
  per-tenant deployments.
- `Primary use cases:` Tenant-scoped synchronous data access; tenant-aware service-to-service calls;
  tenant-preserving CDC, scheduled, and Azure Functions processing; Cosmos DB backfill and strict-mode
  activation; runtime selection of Key Vault-backed Mongo configuration; tenant-aware blob, vault, and email
  routing; environment-by-environment deployment consolidation and rollback.
- `Target users / actors:` Selfcare backend microservices; APIM; service-to-service clients; CDC consumers;
  schedulers; Azure Functions; Terraform pipelines; migration operators; end users of
  `selfcare.pagopa.it` (`AR`) and `imprese.notifichedigitali.it` (`PNPG`).
- `Runtime environment:` Microsoft Azure. HTTP services run as containers in internal Azure Container App
  Environments with private networking, health probes, configurable workload profiles, and Log Analytics
  integration. Azure Functions host function workloads. APIM is the HTTPS ingress. Cosmos DB for MongoDB,
  Azure Storage, and Azure Key Vault are downstream platform services.
- `Server framework:` Java 17 Maven monorepo. Services use Quarkus, including REST/RESTEasy Reactive,
  SmallRye JWT, MongoDB Panache, and Mutiny where applicable; some services use Spring Boot and Spring Data
  MongoDB. Function workloads use Quarkus Azure Functions and the Azure Functions Java libraries. Framework
  versions differ by application and are not standardized by this step.
- `Client framework:` Existing React frontends for the AR and PNPG user journeys. No client-framework change
  is required by Step 2; frontend behavior is outside this step except for continuing to call the existing
  APIM endpoints.
- `API style and integration model:` HTTPS REST APIs described through OpenAPI and exposed through APIM;
  private REST calls between services; asynchronous CDC/event payloads; scheduled batch processing; Azure
  Functions orchestration. APIM resolves the trusted tenant at ingress and overwrites `X-Tenant-Id`.
- `Authentication and session model:` JWT bearer authentication. The backend reconciles the JWT
  `tenant_id` claim with the APIM-provided `X-Tenant-Id` and exposes the result through `TenantContext`.
  Direct service calls propagate a compatible header and JWT; APIM-routed machine clients require
  subscription-to-tenant binding. Async work carries `tenantId` in its persisted payload or uses a
  tenant-bound machine credential.
- `Data model expectations:` Tenant-owned Mongo documents carry a mandatory `tenantId` discriminator after
  migration. Product documents and contract templates are tenant-scoped through
  the API path and persisted discriminator; the IAM role catalogue remains
  unscoped. Product-driven dedicated databases are orthogonal to tenant isolation and retain the discriminator. Tenant-owned blobs use tenant-specific
  containers or paths; shared templates remain explicitly global. Tenant resource metadata and Key Vault
  secret references are represented by the canonical tenant registry.
- `Deployment model:` Terraform-managed Azure resources under `infra/core` and `infra/resources`. The target
  is one eligible Container App deployment per application and environment, reached by both tenant routes.
  Migration proceeds in dependency order while legacy `-ar` and `-pnpg` resources remain available for
  rollback. Terraform state ownership must be transferred before a shared stack manages an existing resource.
- `Scale expectations:` Container Apps expose configurable minimum/maximum replicas and cron, HTTP,
  Azure Queue, or custom scale rules. Cosmos DB supports configured throughput/autoscale, session consistency,
  continuous backup, and automatic failover. Combined workload baselines, tenant rate limits, replica bounds,
  SLOs, RTO, and RPO are `TO BE DECIDED`.
- `Security expectations:` Tenant and resource resolution are fail-closed. Cosmos DB Mongo authentication
  uses connection strings or equivalent Mongo credentials stored in Azure Key Vault, not Managed Identity.
  The Container App identity may retrieve Key Vault references, but that identity is not the Cosmos DB Mongo
  authentication mechanism. Secret values never enter the tenant registry or plaintext Terraform
  configuration. Cross-tenant access, default-resource fallback, and logging of secrets or PII are forbidden.

## Initial Architecture (Provisional)

**Assumption A:** Step 0 tenant validation remains the only HTTP trust boundary. Step 2 consumes
`TenantContext`; it does not introduce another header, claim, hostname, or path parser.

**Assumption B:** The Step 1 canonical registry is the configuration-plane source for tenant resource
metadata. No service-specific tenant map is introduced.

**Assumption C:** A shared deployment may temporarily need multiple Cosmos DB connection configurations while
legacy accounts coexist or a resource is dedicated. The final number of accounts per service is determined by
the migration plan, not by this architecture.

**Assumption D:** Personal Data Vault and email routing remain disabled for any tenant whose mapping is
incomplete. Their provider-specific client design is `TO BE DECIDED`.

### 1. Trusted tenant context

APIM maps the incoming tenant route to `AR` or `PNPG`, overwrites `X-Tenant-Id`, and forwards the request.
The backend validates the header against the JWT claim and stores the accepted value in `TenantContext`.
Repositories and downstream resource providers receive only this validated value.

For non-request workloads, the tenant is part of the durable work item:

- service-to-service calls propagate the validated tenant;
- CDC events and function activity payloads carry `tenantId`;
- scheduled records retain `tenantId` through delivery;
- machine credentials that cannot carry user context are bound to one tenant.

Missing or inconsistent context is rejected before resource selection.

### 2. Tenant resource registry

`local.tenant_data_isolation` defines non-secret routing metadata for each tenant and resource dimension.
Terraform serializes the required metadata for applications and separately maps configuration keys to Azure
Key Vault secret names. The application-facing registry contains identifiers and secret references only.

`TenantDataIsolationRegistry` is the application boundary for looking up:

- Cosmos account/database metadata and connection-secret reference;
- a map of logical storage bindings, each containing account/container, optional path prefix, authentication
  mode, and secret-backed environment-variable references where required;
- Personal Data Vault tenant identifier;
- email sender domain and secret references.

An unknown tenant, absent registry entry, null required dimension, or invalid configuration produces a typed
failure. There is no default tenant or default resource.

**Registry storage and schema.** The application-facing registry is a single non-secret configuration value
(for example an environment variable populated with a JSON document, following the existing
`tenant.registry.json` pattern already used by other services), holding one object keyed by tenant code. Each
tenant entry groups its resource dimensions. `storages` is a map keyed by application-owned logical names,
not an array: callers request a binding such as `products`, while the registry decides which physical
account/container serves that purpose for the current tenant. The Mongo and Storage dimensions carry only
resource identifiers and environment-variable references, never secret values:

```json
{
  "AR": {
    "mongo": {
      "account": "cosmos-ar",
      "database": "selcOnboarding",
      "connectionStringEnvVar": "MONGODB_CONNECTION_STRING_AR"
    },
    "jwt": {
      "publicKeyEnvVar": "JWT_PUBLIC_KEY_AR"
    },
    "storages": {
      "products": {
        "account": "stselcarproducts",
        "container": "selc-d-product",
        "pathPrefix": "",
        "authentication": {
          "type": "MANAGED_IDENTITY",
          "managedIdentityClientIdEnvVar": "AZURE_CLIENT_ID_AR_PRODUCTS"
        }
      },
      "contracts": {
        "account": "stselcardocuments",
        "container": "contracts",
        "pathPrefix": "onboarding",
        "authentication": {
          "type": "CONNECTION_STRING",
          "connectionStringEnvVar": "BLOB_CONNECTION_STRING_AR_CONTRACTS"
        }
      }
    }
  },
  "PNPG": {
    "mongo": {
      "account": "cosmos-pnpg",
      "database": "selcOnboarding",
      "connectionStringEnvVar": "MONGODB_CONNECTION_STRING_PNPG"
    },
    "jwt": {
      "publicKeyEnvVar": "JWT_PUBLIC_KEY_PNPG"
    },
    "storages": {
      "products": {
        "account": "stpnpgproducts",
        "container": "selc-d-product",
        "pathPrefix": "",
        "authentication": {
          "type": "MANAGED_IDENTITY",
          "managedIdentityClientIdEnvVar": "AZURE_CLIENT_ID_PNPG_PRODUCTS"
        }
      }
    }
  }
}
```

Only Terraform knows actual Key Vault secret names. It maps each secret to a Container App secret and then to
the environment variable named in the registry. For `MANAGED_IDENTITY`, the account is non-secret metadata and
the optional client ID is read from the referenced environment variable. For `CONNECTION_STRING`, the
referenced environment variable is mandatory and secret-backed.

The registry loader validates normalized tenant IDs, logical storage-key uniqueness, mandatory fields,
supported authentication types, and mutually exclusive authentication settings. Adding a tenant, storage
purpose, or physical account is a registry and infrastructure change; the application never calls Key Vault
directly.

### 3. Cosmos DB Mongo routing and isolation

Cosmos DB uses the Mongo API over private networking. Mongo clients authenticate with connection strings
stored in Azure Key Vault; Managed Identity is not used for Mongo authentication.

The deployment path is:

1. Terraform declares a distinct application secret/configuration key for every required Cosmos route and
   records the corresponding environment variable name as that route's `connectionStringEnvVar` in the
   tenant registry (see §2).
2. The Container App module creates a Key Vault-backed Container App secret using its assigned identity.
3. The secret is exposed to the container as a secret-backed environment variable, named exactly as declared
   in step 1.
4. The application validates the configured routes and initializes the required Mongo clients at startup by
   reading the already-resolved environment variable named in the registry; it never calls Key Vault, holds
   Key Vault credentials, or resolves a Key Vault secret name itself.
5. Each operation selects the client/database from the validated tenant and, when applicable, product routing.

The runtime selector never fetches Key Vault on each data operation and never embeds connection strings in
the registry. A missing or invalid secret fails startup when the route is mandatory, or fails the affected
route before data access when it is intentionally deferred. Secret rotation is completed through the
deployment platform's secret refresh/revision procedure without changing registry schema or application code.

Within the selected database, tenant-owned entities are isolated by `tenantId` at the repository or shared
query-builder chokepoint. Global catalogues use explicit unscoped access paths. Product-driven database
routing is evaluated before opening the database, but it does not replace the tenant discriminator.

The migration sequence per environment is:

1. backfill legacy documents separately in the AR and PNPG accounts;
2. verify all expected collections for both tenants;
3. enable strict tenant filtering consistently across all participating services;
4. migrate tenant-qualified indexes and reconcile conflicting data;
5. import data into the selected shared/dedicated target;
6. route traffic only after isolation and functional checks pass;
7. remove the null-compatible predicate and temporary flag after all environments are strict.

### 4. Azure Storage routing

A tenant-aware storage provider resolves a pair `(tenantId, logicalStorageKey)`:

```text
TenantContext + code-owned key ("products")
                    ↓
TenantRegistry.storage(tenantId, logicalStorageKey)
                    ↓
account + container + pathPrefix + authentication
                    ↓
cached AzureBlobClient for the selected binding
```

The logical key is a stable application contract. `onboarding-ms` initially uses `products`; future purposes
are added under the same `storages` map. A tenant can bind `products` and `contracts` to different accounts,
while another tenant can bind both to one account. The caller does not know or select the physical topology.

Clients are created once per distinct account/credential configuration and stored in an immutable registry.
Container and trusted path-prefix metadata remain part of the logical binding and are applied per operation.
No request-scoped tenant or selected client is stored in application-global mutable state.

Authentication modes:

- `MANAGED_IDENTITY`: construct the endpoint from `account`; optionally use the user-assigned identity client
  ID read from `managedIdentityClientIdEnvVar`;
- `CONNECTION_STRING`: read the value from the secret-backed `connectionStringEnvVar`;
- additional modes require a schema and security review before use.

`onboarding-ms` replaces the flat `onboarding-ms.blob-storage.*-product` selection path with the tenant-aware
`products` binding. The product filename (currently `products.json`) remains application configuration because
it identifies a domain asset, while account/container/authentication are tenant-resource routing metadata.

Tenant-owned `document-ms` operations use tenant-specific logical bindings. Shared templates use separate
typed operations or explicit call-site classification and an explicitly configured shared binding.
Client-supplied blob paths never decide the logical key or whether an operation uses shared or tenant storage.
Missing mappings, credentials, containers, or permissions fail closed without falling back to legacy flat
properties.

The ownership and provisioning mechanism for tenant containers is `TO BE DECIDED`. The classification of
`dashboard-bff` institution logos is also `TO BE DECIDED`.

### 5. Personal Data Vault and email routing

Vault and email providers consume the same validated tenant and canonical registry:

- the vault provider selects the configured vault instance/logical tenant and rejects an unset mapping;
- the email provider selects tenant-compatible sender metadata and credentials per notification;
- secrets remain in Key Vault and only non-secret metadata or secret references appear in the registry;
- logs contain tenant, outcome, and correlation identifiers only, never payloads, tokens, or credentials.

Provider/API details, complete caller inventories, vault tenant values, sender domains, and tenant-specific
machine credentials are `TO BE DECIDED`.

### 6. Consolidation boundary

Each shared deployment is treated as a migration boundary. It is eligible for cutover only after inbound
validation, outbound propagation, data isolation, resource configuration, network reachability, and rollback
have been verified for both tenants.

Services are migrated according to the internal call graph: dependencies before callers and BFF/gateway
entry points last. Existing Azure resources are moved to exactly one Terraform state before the unified stack
manages them. Legacy resources remain available until production validation succeeds.

## Requirement Traceability

| Architecture component or boundary | Requirement groups | Notes |
|---|---|---|
| APIM tenant resolution and backend `TenantContext` | SELC-12.1, SELC-12.5 | Reuses the Step 0 trust boundary; no tenant re-derivation downstream |
| Tenant propagation for REST, CDC, schedulers, and Functions | SELC-12.2-SELC-12.4 | Tenant-bearing payload and machine-credential conventions must be completed before workload consolidation |
| Canonical tenant resource registry | SELC-13.10, SELC-14.1, SELC-15.2-SELC-15.3, SELC-16.2-SELC-16.3, SELC-17.3-SELC-17.4 | Single non-secret configuration plane with fail-closed lookup |
| Key Vault-backed Cosmos configuration | SELC-13.9-SELC-13.14, SELC-17.2, SELC-17.5-SELC-17.6 | Container App identity retrieves the secret; Mongo authenticates with the resulting connection string, not Managed Identity |
| Tenant/product-aware Mongo client and database selector | SELC-13.1, SELC-13.8, SELC-13.12-SELC-13.13 | `onboardingId`-only dedicated-database lookup still needs architecture input |
| Strict discriminator and migration pipeline | SELC-13.1-SELC-13.7 | Backfill, verification, strict activation, index migration, and import are ordered gates |
| Tenant/logical-key storage registry and cached client provider | SELC-14.1-SELC-14.10 | `onboarding-ms` starts with `products`; the schema supports additional accounts and purposes per tenant |
| Typed shared/tenant storage operations | SELC-14.5, SELC-14.7-SELC-14.12 | Container provisioning ownership and `dashboard-bff` logo classification need architecture input |
| Personal Data Vault provider | SELC-15 | Provider, API, caller inventory, tenant identifiers, and credentials are `TO BE DECIDED` |
| Tenant-aware email provider and scheduler | SELC-16 | Sender mapping, complete producer inventory, and tenant-bound machine credentials are `TO BE DECIDED` |
| Shared deployment configuration boundary | SELC-17 | Prevents tenant-specific values from being collapsed into legacy single-value settings |
| Dependency-ordered cutover and Terraform state transfer | SELC-18 | Rate limits, replica bounds, SLOs, RTO, and RPO need more architecture input |

## Dependency Rules

- Do not add a dependency when the standard library or a few lines of first-party code will do.
- Prefer zero new dependencies. If a library is required, justify it in the PR description.
- Only use libraries that are actively maintained (commit or release within the last 12 months).
- Only use the latest stable major version. No deprecated, abandoned, or pre-release packages.
- Reject any library with known unpatched CVEs. Check before adding and on every update.
- Audit transitive dependencies, not just direct ones. A small direct dep with a large or unvetted tree is a rejection.
- Pin exact versions with a committed lockfile. No floating ranges in production.
- Prefer libraries with a narrow scope, minimal dependencies of their own, and a clear security track record.
