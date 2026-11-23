# selfcare-sdk-tenant

> Shared tenant context and runtime tenant registry (AR, PNPG, ...) for SelfCare Quarkus services: it maps each tenant to its MongoDB, JWT key, blob storages and external credentials.

| | |
|---|---|
| Path | `libs/selfcare-sdk-tenant` |
| Artifact | `it.pagopa.selfcare:selfcare-sdk-tenant:0.7.0` |
| Stack | Plain Java 17 + CDI (Jakarta), MicroProfile Config, Jackson; parent [`selfcare-sdk-pom`](selfcare-sdk-pom.md) (Quarkus BOM 3.11.2) |
| Used by | `auth`, `document-ms`, `iam`, `onboarding-ms`, `product`, `product-cdc`; transitively through [`selfcare-sdk-security`](selfcare-sdk-security.md) and [`selfcare-sdk-tenant-mongodb`](selfcare-sdk-tenant-mongodb.md) |

## What it provides

Package `it.pagopa.selfcare.tenant` (`libs/selfcare-sdk-tenant/src/main/java/it/pagopa/selfcare/tenant/`):

| Class | Purpose |
|---|---|
| `TenantContext` | `@RequestScoped` holder of the tenant resolved for the current request/operation. `requiredTenantId()` throws `UnresolvedTenantException` when empty. |
| `TenantRegistry` | `@ApplicationScoped` registry parsed at startup from JSON config. Resolves a tenant (`resolve`), normalizes IDs (trim + upper case), exposes Mongo connection string, per-tenant JWT public key, logical blob storages, OneIdentity, UserRegistry and signature (Namirial/Aruba) credentials. |
| `TenantDefinition` | Record mapped from the registry JSON: `mongo`, `jwt`, `storages`, `oneIdentity`, `userRegistry`, `signature`. |
| `StorageAuthenticationType` | `MANAGED_IDENTITY` or `CONNECTION_STRING` for a storage definition. |
| `UnknownTenantException`, `UnresolvedTenantException`, `UnknownStorageException` | Errors for unsupported tenant, missing tenant in context, unknown logical storage key. |

The registry JSON never contains secrets: every sensitive value is referenced by the **name** of an
environment variable/config key (`connectionStringEnvVar`, `publicKeyEnvVar`, `apiKeyEnvVar`,
`clientIdEnvVar`, ...), which `TenantRegistry` reads through MicroProfile Config at runtime.
Validation of the definitions happens at startup (`@PostConstruct`).

The jar ships `META-INF/beans.xml` (`bean-discovery-mode="annotated"`), so beans are discovered by Quarkus.

## Usage

```xml
<dependency>
  <groupId>it.pagopa.selfcare</groupId>
  <artifactId>selfcare-sdk-tenant</artifactId>
  <version>0.7.0</version>
</dependency>
```

Configuration properties read by `TenantRegistry`:

| Property | Default | Meaning |
|---|---|---|
| `tenant.registry.json` | `{}` | Map `tenantId -> TenantDefinition` (JSON). |
| `tenant.resources.registry.json` | (none) | Optional alternative registry for tenant resources. |
| `tenant.supported-tenants` | `*` | Comma-separated tenant IDs; `*` means all keys of the registry. |
| `tenant.mongo.mandatory` | `true` | Disable only for apps that use tenant resources without Mongo. |
| `tenant.storage.mandatory-keys` | (none) | Logical storage keys that every supported tenant must define. |
| `tenant.one-identity.mandatory-tenants` | (none) | Tenants that must configure OneIdentity. |
| `tenant.user-registry.mandatory-tenants` | (none) | Tenants that must configure UserRegistry. |

Shape of a registry entry (abridged from `apps/iam/src/main/resources/application.properties`):

```json
{"AR": {"mongo": {"account": "cosmos-ar", "database": "selcIam",
                  "connectionStringEnvVar": "MONGODB_CONNECTION_STRING_AR"},
        "jwt":   {"publicKeyEnvVar": "...",
                  "session": {"audience": "api.dev.selfcare.pagopa.it"}}}}
```

`jwt.session.audience` (optional, since 0.6.0) is the `aud` claim of the JWTs an app issues for the tenant: read it with `TenantRegistry.jwtSessionAudience(tenantId)`.

`JwtTenantClaimResolver` (since 0.7.0) resolves the tenant of an already verified JWT from its `tenant_id` claim, without depending on a JWT library: `JwtTenantClaimResolver.of(defaultTenant, supportedTenants, tenantClaimRequired).resolve(rawClaim)` returns the normalized tenant and whether it came from the claim or from the default tenant. A missing claim falls back to the default tenant (rejected when there is no default or the claim is required); a blank, non-string or unsupported claim throws `InvalidTenantClaimException`. Used by `selfcare-sdk-security` (`JwtTenantValidator`) and `onboarding-functions` (`HttpJwtValidator`); matching the tenant with `X-Tenant-Id` stays in the caller.

Real examples: `apps/iam`, `apps/onboarding-ms`, `apps/document-ms`, `apps/product-cdc` (`src/main/resources/application.properties`), `apps/auth` (`TENANT_REGISTRY_JSON`).
Request filters that populate `TenantContext` are provided by [`selfcare-sdk-security`](selfcare-sdk-security.md).

## Build and release

```bash
mvn -f libs/selfcare-sdk-tenant/pom.xml clean verify
mvn --projects :selfcare-sdk-tenant --also-make-dependents clean package -DskipTests   # lib + dependents
```

Released to GitHub Packages (`https://maven.pkg.github.com/pagopa/selfcare`, from the parent POM) by
`.github/workflows/release-sdk.yml` and `.github/workflows/release_sdk_pom.yml`: both run on push to `main`
touching security/tenant/tenant-mongodb and on `workflow_dispatch` with a `module` input (`all` by default),
building with `mvn --projects :<module> --also-make clean install` and deploying with `mvn deploy -f libs/<module>/pom.xml`.
PR checks: `.github/workflows/pr_libs.yml` (code review for `libs/**`).

## References

- [selfcare-sdk-tenant-mongodb](selfcare-sdk-tenant-mongodb.md), [selfcare-sdk-security](selfcare-sdk-security.md)
- Spring counterpart: `tenant` module of [selfcare-commons](selfcare-commons.md)
- [Multitenancy](../multitenancy.md), [Multitenant Step 2 architecture](../specs/MULTITENANT/Step_2/ARCHITECTURE.md)
