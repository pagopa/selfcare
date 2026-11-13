# selfcare-sdk-security

> Quarkus security SDK: custom JWT validation (issuers `SPID`/`PAGOPA`, per-tenant keys), role augmentation, JWT tenant validation against `X-Tenant-Id`, opt-in tenant resolution and MDC logging enrichment.

| | |
|---|---|
| Path | `libs/selfcare-sdk-security` |
| Artifact | `it.pagopa.selfcare:selfcare-sdk-security:0.8.2` |
| Stack | Quarkus (`quarkus-smallrye-jwt`, `quarkus-rest-jackson`, `quarkus-rest-client-jackson`, BOM 3.11.2), Java 17, `java-jwt`, `jose4j`; parent [`selfcare-sdk-pom`](selfcare-sdk-pom.md); depends on [`selfcare-sdk-tenant`](selfcare-sdk-tenant.md) |
| Used by | `document-ms`, `iam`, `onboarding-ms`, `product`, `user-ms`, `webhook` |

## What it provides

Package `it.pagopa.selfcare.security` (`libs/selfcare-sdk-security/src/main/java/it/pagopa/selfcare/security/`):

| Class | Purpose |
|---|---|
| `JWTCallerPrincipalFactory` | CDI `@Alternative` replacing the Quarkus JWT parser. Accepts only issuers `SPID` and `PAGOPA`; verifies with `mp.jwt.verify.publickey` and/or the per-tenant keys returned by `TenantRegistry.jwtPublicKey(...)`. |
| `JWTSecurityIdentityAugmentor` | Adds roles to the `SecurityIdentity`; tokens issued by `PAGOPA` get the `SUPPORT` role. |
| `JwtTenantValidator`, `JwtTenantValidationFilter` | For `SPID` tokens, reconcile the `tenant_id` claim (resolved by `selfcare-sdk-tenant` `JwtTenantClaimResolver`) with the `X-Tenant-Id` header (`401` on missing/unknown/mismatching header). |
| `LoggingContextFilter` | Copies the JWT claims listed in `selfcare.logging.mdc.claims` (default `uid`) into the MDC for every REST request. |
| `tenant.TenantResolutionFilter` | Opt-in JAX-RS filter resolving the tenant from `tenantId` query param / `X-Tenant-Id` header, validating it with `TenantRegistry` and storing it in `TenantContext` (`400` problem on conflict). |
| `tenant.EffectiveTenant` | Resolves an endpoint's optional `tenantId` param, falling back to `TenantContext` then to the JWT `tenant_id` claim. |
| `tenant.TenantLogUtils` | Log-safe read of `X-Tenant-Id`. |

## Usage

```xml
<dependency>
  <groupId>it.pagopa.selfcare</groupId>
  <artifactId>selfcare-sdk-security</artifactId>
  <version>0.8.2</version>
</dependency>
```

Main configuration (full details in `libs/selfcare-sdk-security/README.md`):

| Property / env var | Default | Meaning |
|---|---|---|
| `mp.jwt.verify.publickey` | `NONE` | Shared public key (legacy, single key). |
| `DEFAULT_TENANT` (env) | `PNPG` | Tenant of SPID tokens without `tenant_id`. |
| `SUPPORTED_TENANTS` (env) | `AR,PNPG` | Tenants accepted in claim and header. |
| `JWT_TENANT_CLAIM_REQUIRED` (env) | `false` | Reject SPID tokens without `tenant_id`. |
| `selfcare.security.tenant-validation.enabled` | `true` | Build time; disables `JwtTenantValidationFilter`. |
| `selfcare.tenant.resolution-filter.enabled` | `false` | Build time; registers `TenantResolutionFilter`. |
| `selfcare.tenant.resolution-filter.query-param-enabled` | `true` | Read the `tenantId` query parameter. |
| `tenant.enforcement.enabled` / `tenant.default` | `true` / `AR` | Fallback when no tenant is sent. |
| `selfcare.logging.mdc.claims` | `uid` | Claims copied to the MDC. |

Non tenant-aware apps can exclude the tenant beans with `quarkus.arc.exclude-types=...` and
`selfcare.security.tenant-validation.enabled=false` (see the README section "Using the SDK in a non tenant-aware application").

## Build and release

```bash
mvn -f libs/selfcare-sdk-security/pom.xml clean verify
mvn --projects :selfcare-sdk-security --also-make-dependents clean package -DskipTests
```

Release workflows (GitHub Packages `https://maven.pkg.github.com/pagopa/selfcare`):

- `.github/workflows/release_security_sdk.yml` ("Release Security sdk"): push on `main` or `workflow_dispatch`, `mvn -B deploy -f libs/selfcare-sdk-security/pom.xml`.
- `.github/workflows/release-sdk.yml`: manual matrix release of security, tenant, tenant-mongodb and m2m.

Coverage: profile `selfcare-sdk-security` in `test-coverage/pom.xml`.

## References

- [`libs/selfcare-sdk-security/README.md`](../../libs/selfcare-sdk-security/README.md)
- [selfcare-sdk-tenant](selfcare-sdk-tenant.md), [Multitenancy](../multitenancy.md)
- Spring counterpart (JWT filter and strategies): `web` module of [selfcare-commons](selfcare-commons.md)
