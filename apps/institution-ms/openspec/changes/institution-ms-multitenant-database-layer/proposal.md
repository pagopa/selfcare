# Proposal

## Why

`institution-ms` currently uses one MongoDB binding and unscoped persistence operations, so forwarding `X-Tenant-Id` does not isolate institutions, delegations, or mail notifications. Adopt the model already used by `user-group-ms`: shared Spring tenant resolution and MongoDB routing, persisted tenant ownership, and tenant-scoped queries.

## What Changes

- Integrate `selc-commons-tenant` and the tenant-aware commons web stack, using `TenantConfiguration`, `TenantMongoConfiguration`, and the existing authenticated tenant context rather than introducing a service-specific router.
- Configure resource bindings through `SELFCARE_TENANT_DATA_ISOLATION`, with the supported-tenant allowlist and default-tenant settings used by `user-group-ms`; preserve institution-specific MongoDB environment variables for the single-tenant setup.
- Persist an internal `tenantId` on `Institution`, `Delegations`, and `MailNotification`. Scope reads, counts, existence checks, mutations, upserts, and aggregation joins to the resolved tenant.
- Support `TENANT_STRICT_DATA_ISOLATION`, defaulting to `true`. Explicit compatibility mode includes untagged legacy documents only inside the selected tenant database; documents belonging to another tenant remain excluded.
- Make MongoDB readiness probe every supported tenant through the routed template, without requiring an HTTP request context.
- Preserve collection names, IDs, domain DTOs, pagination, onboarding projections, and custom `OffsetDateTime` conversions. Adapt existing fixtures and add two-tenant acceptance coverage.
- Update the six Terraform deployments under `infra/resources/institution-ms` (DEV/UAT/PROD for AR and PNPG) using the `user-group-ms` deployment pattern: explicit supported/default tenant, registry, enabled MongoDB routing, strict isolation, and tenant-specific MongoDB/JWT references to the existing Key Vault secrets. Each deployment retains its current tenant, Cosmos DB account, and `selcMsCore` database and the existing Mongo client application name.
- **BREAKING**: strict mode hides existing documents without `tenantId`; data backfill or an explicitly configured compatibility window is required before rollout. Enabling the shared registry also activates the reference service's tenant-header/JWT validation contract.

## Capabilities

### New Capabilities

- `institution-tenant-persistence`: Tenant resolution at the persistence boundary, registry-based MongoDB routing, document ownership and query isolation, legacy-data compatibility, tenant-aware database readiness, and Terraform deployment configuration for `institution-ms`.

### Modified Capabilities

None. The OpenSpec capability inventory is currently empty.

## Impact

- Application bootstrap and Maven dependency management in `apps/institution-ms`, including a compatible, aligned commons release containing the shared tenant implementation.
- The three DAO entities, repositories, connectors, custom Mongo operations, mapping configuration, and MongoDB readiness indicator under `apps/institution-ms/connector/dao`.
- Application configuration, integration fixtures, request coverage, and operational documentation under `apps/institution-ms/app` and `apps/institution-ms/README.md`.
- Terraform application settings and secret references in `infra/resources/institution-ms/{dev,uat,prod}-{ar,pnpg}/main.tf`, following the corresponding `user-group-ms` deployments without changing database resources, collection/index definitions, networking, identities, storage, or other service settings.
- The existing Spring Mongo integration in `libs/selfcare-commons/tenant` to preserve custom conversions and support an optional configured Mongo client application name; no replacement tenant SDK or Quarkus migration.
- Operators must supply per-tenant MongoDB and JWT key references, prepare existing data, and verify readiness before serving multiple tenants.
- Out of scope: tenant-aware Azure Storage, email-provider routing, downstream-service redesign, unrelated business-rule changes, provisioning new infrastructure resources, automated production data migration, and changing shared authentication rules. Updating the existing institution deployments' tenant configuration is in scope.
