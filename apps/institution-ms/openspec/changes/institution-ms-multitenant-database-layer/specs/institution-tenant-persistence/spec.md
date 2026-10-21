# Spec Delta

## Purpose

Isolate institution, delegation, and mail-notification persistence by the authenticated tenant, using the same resource-routing and document-ownership model as `user-group-ms`, with explicit legacy-data compatibility and database readiness.

## ADDED Requirements

### Requirement: Authenticated tenant determines persistence scope

The service SHALL use the tenant resolved by the shared authentication contract for all request-driven persistence. Header normalization, supported-tenant validation, verified JWT tenant matching, and strategy-specific defaults SHALL match `user-group-ms`. Persistence without a resolved tenant MUST fail rather than select an arbitrary database.

#### Scenario: Valid tenant request

- **WHEN** an authenticated request has `X-Tenant-Id: ar` and a verified JWT accepted for tenant `AR`
- **THEN** every persistence operation in that request uses tenant `AR`

#### Scenario: Unsupported tenant selection

- **WHEN** a JWT-authenticated request supplies a tenant outside the supported-tenant allowlist
- **THEN** authentication fails with HTTP 401 and performs no domain persistence

#### Scenario: Duplicate or mismatched tenant selection

- **WHEN** a JWT-authenticated request supplies multiple tenant headers or a header that conflicts with its verified JWT tenant
- **THEN** the service returns HTTP 400 with an `application/problem+json` response and performs no domain persistence

#### Scenario: Missing tenant header on JWT authentication

- **WHEN** an otherwise valid JWT-authenticated request omits the tenant header required by its authentication strategy
- **THEN** tenant-specific key resolution fails with HTTP 401 and performs no domain persistence

#### Scenario: Internal access without context

- **WHEN** a domain persistence operation runs without an explicitly resolved tenant
- **THEN** the operation fails with an unresolved-tenant error before accessing domain data

#### Scenario: Context does not survive request completion

- **WHEN** a tenant request succeeds or fails and the same worker subsequently handles another request
- **THEN** no tenant context from the completed request remains available to the subsequent request

### Requirement: Tenant registry controls MongoDB bindings

The service SHALL select the MongoDB connection and database from the supported tenant's `SELFCARE_TENANT_DATA_ISOLATION` entry. Registry entries MUST reference externally supplied connection values rather than contain credentials. Existing institution MongoDB environment settings SHALL remain usable for the default single-tenant setup.

#### Scenario: Two tenants use distinct databases

- **WHEN** `AR` and `PNPG` are configured with different MongoDB databases and each creates an institution with the same ID
- **THEN** each tenant reads and modifies only its own institution in its configured database

#### Scenario: Connection and database are both selected

- **WHEN** supported tenants use different MongoDB accounts or connections
- **THEN** each tenant's operations use its own connection as well as its own configured database name

#### Scenario: Existing single-tenant connection settings

- **WHEN** only tenant `AR` is enabled using the documented single-tenant configuration and the existing `MONGODB_CONNECTION_URI`, `MONGODB_APPNAME`, and `MONGODB_NAME_SELC_MSCORE` settings
- **THEN** routed persistence uses the same configured institution database and connection options

### Requirement: Terraform deployments supply domain-specific tenant bindings

The six existing DEV/UAT/PROD AR/PNPG Terraform roots for `institution-ms` SHALL supply explicit domain tenant settings, enabled MongoDB routing, strict isolation, and a credential-free registry following `user-group-ms`. Each deployment MUST retain its existing tenant, Cosmos DB account, `selcMsCore` database, and unrelated infrastructure, with MongoDB/JWT references resolving through its existing Key Vault.

#### Scenario: AR deployments retain the AR binding

- **WHEN** a DEV, UAT, or PROD AR institution deployment is configured through Terraform
- **THEN** its supported/default tenant is `AR`, its registry contains only `AR`, and its MongoDB binding selects the existing deployment's `selcMsCore` database using `MONGODB_CONNECTION_STRING_AR` and `JWT_PUBLIC_KEY_AR`

#### Scenario: PNPG deployments do not inherit the AR default

- **WHEN** a DEV, UAT, or PROD PNPG institution deployment is configured through Terraform
- **THEN** its supported/default tenant is `PNPG`, its registry contains only `PNPG`, and its MongoDB binding selects the existing deployment's `selcMsCore` database using `MONGODB_CONNECTION_STRING_PNPG` and `JWT_PUBLIC_KEY_PNPG`

#### Scenario: Tenant references resolve through existing secrets

- **WHEN** the Container App receives its domain-specific MongoDB and JWT environment references
- **THEN** those references use the existing `mongodb-connection-string` and `jwt-public-key` secrets from that deployment's existing Key Vault, without credentials in registry JSON or ordinary application settings and without duplicate Container App secret declarations

#### Scenario: Routing and strict isolation are explicit

- **WHEN** any of the six deployments uses the Terraform baseline without an explicitly authorized compatibility override
- **THEN** `TENANT_MONGODB_ENABLED` and `TENANT_STRICT_DATA_ISOLATION` are `true`, and untagged legacy data requires preparation before rollout rather than automatic compatibility mode

#### Scenario: Tenant wiring preserves existing infrastructure and connection behavior

- **WHEN** the six Terraform tenant-configuration source changes are reviewed
- **THEN** the existing effective MongoDB URI options and application name are preserved, and database/collection resources, indexes, locks, state addresses, networking, identities, storage, and unrelated service settings are unchanged

### Requirement: Invalid tenant resource configuration prevents startup

The service MUST fail startup when a supported tenant is missing its MongoDB definition, has an invalid secret reference, or references an absent or blank connection value. Tenant-specific JWT verification key references required by the shared authentication contract MUST also resolve before requests are served. Diagnostics MUST NOT expose secret values.

#### Scenario: Missing tenant binding

- **WHEN** the supported-tenant allowlist includes `PNPG` but its registry entry has no MongoDB definition
- **THEN** startup fails with a configuration error identifying the tenant

#### Scenario: Missing connection value

- **WHEN** a supported tenant references an absent or blank MongoDB connection environment value
- **THEN** startup fails rather than falling back to another tenant or the legacy connection

#### Scenario: Missing JWT verification key reference

- **WHEN** the documented tenant configuration requires a JWT verification key whose external reference is absent or blank
- **THEN** startup fails without logging the key or MongoDB credentials

### Requirement: Persisted documents carry server-controlled tenant ownership

New institution, delegation, and mail-notification documents MUST persist the resolved `tenantId`, including documents created by upsert. Replacement writes MUST retain that ownership. Client payloads MUST NOT select or change tenant ownership, and a write MUST NOT replace an existing document explicitly owned by another tenant.

#### Scenario: Create and replace owned entities

- **WHEN** tenant `AR` creates or replaces an institution or delegation
- **THEN** the stored document has `tenantId: AR`, independently of client-supplied fields

#### Scenario: Upsert a mail notification

- **WHEN** tenant `PNPG` adds a mail notification for an institution with no matching notification
- **THEN** the inserted notification has `tenantId: PNPG`

#### Scenario: Conflicting stored ownership

- **WHEN** an `AR` write targets an ID already occupied by a document marked `PNPG` in the selected database
- **THEN** it does not replace, relabel, or mutate that document, and the operation surfaces failure

### Requirement: All domain queries enforce tenant ownership

In strict mode, all institution, delegation, and mail-notification reads, counts, existence checks, updates, and deletes SHALL match only documents whose `tenantId` equals the resolved tenant. This MUST include ID-based, bulk, derived, projected, and custom queries. Existing domain filters and not-found behavior SHALL remain applicable within that scope.

#### Scenario: Foreign document in selected database

- **WHEN** an `AR` request queries, counts, updates, or deletes a document marked `PNPG` in the selected database
- **THEN** that document is excluded and remains unchanged

#### Scenario: Lists and pagination

- **WHEN** tenant `AR` lists institutions or delegations in a database containing documents with multiple tenant markers
- **THEN** returned items and total counts include only matching `AR` documents and obey the existing filters, ordering, and pagination

#### Scenario: Nested onboarding mutation

- **WHEN** an `AR` request changes an onboarding product on an institution marked `PNPG`
- **THEN** neither the nested onboarding nor the institution timestamps are changed

#### Scenario: Mail-notification cleanup

- **WHEN** tenant `AR` removes the last product from its mail notification
- **THEN** only the matching `AR` notification is updated or removed, and foreign notifications remain unchanged

### Requirement: Aggregations isolate both source and joined documents

Aggregation results, totals, and joined institution data SHALL obey the same tenant-ownership rules as ordinary queries. Tenant filtering MUST apply before counts and page selection, and independently to joined or graph-traversed documents, so an owned source cannot expose a foreign joined document.

#### Scenario: Delegation lookup finds a foreign institution

- **WHEN** an `AR` delegation references an institution marked `PNPG` in the selected database
- **THEN** delegation lookup results expose no fields from that foreign institution

#### Scenario: Delegation aggregate count

- **WHEN** tenant `AR` requests delegation counts or a date-based paginated aggregation
- **THEN** foreign documents do not affect totals, groups, or returned pages

#### Scenario: Custom graph lookup

- **WHEN** an `AR` aggregation traverses bindings to institutions in the selected database
- **THEN** both source documents and traversed institutions satisfy the active tenant-ownership policy

### Requirement: Legacy visibility requires explicit compatibility mode

`TENANT_STRICT_DATA_ISOLATION` SHALL default to `true`. When explicitly `false`, queries SHALL also match documents with absent or null `tenantId` inside the selected database. Compatibility mode MUST NOT include explicitly foreign-owned documents, broaden database routing, or allow creation of new untagged documents.

#### Scenario: Strict mode excludes legacy data

- **WHEN** strict isolation is enabled or unspecified and a document has no `tenantId` or has `tenantId: null`
- **THEN** the document is excluded from domain reads, counts, updates, deletes, and aggregation joins

#### Scenario: Explicit legacy compatibility

- **WHEN** compatibility mode is explicitly enabled for a tenant's selected database
- **THEN** queries and mutations can match that tenant's documents and untagged legacy documents, but never documents explicitly marked as another tenant

#### Scenario: New writes during compatibility window

- **WHEN** a new document is inserted or upserted while compatibility mode is enabled
- **THEN** it still receives the resolved tenant marker

### Requirement: Readiness covers every supported tenant database

MongoDB readiness SHALL be UP only when every supported tenant database can be pinged, and DOWN when any such probe fails. Probes MUST work without an HTTP tenant context, restore any previous context after success or failure, and exclude tenants outside the allowlist. Existing readiness component names SHALL remain stable.

#### Scenario: All configured databases respond

- **WHEN** a readiness request has no tenant context and all supported tenant databases respond to ping
- **THEN** `mongoReadiness` reports UP after probing every supported tenant

#### Scenario: One tenant database fails

- **WHEN** one supported tenant database fails its ping
- **THEN** `mongoReadiness` reports DOWN, reports no credentials, and leaves the caller's previous tenant context unchanged

#### Scenario: Unsupported registry entry

- **WHEN** a registry entry exists for a tenant excluded from the supported-tenant allowlist
- **THEN** readiness does not probe that entry and requests cannot select it

### Requirement: Existing persistence representation remains compatible

The service SHALL preserve collection names, document IDs, domain request and response schemas, onboarding projections, and the existing string representation and round-trip behavior of persisted `OffsetDateTime` values. Tenant ownership SHALL remain persistence metadata rather than a new client-controlled API field.

#### Scenario: Date conversion after routing is enabled

- **WHEN** an institution or delegation containing an `OffsetDateTime` is written and read through tenant-routed persistence
- **THEN** its stored date representation and recovered offset-aware value match the existing conversion behavior

#### Scenario: Existing onboarding projection

- **WHEN** a tenant requests onboarding information filtered by institution ID and product ID
- **THEN** the response retains the existing product projection and exposes no new tenant-ownership field
