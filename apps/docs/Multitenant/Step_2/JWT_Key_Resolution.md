# JWT Verification Key Resolution

The JWT verification key used by tenant-aware services is resolved through the same
`TenantRegistry` already used
for Mongo database identification (see `Database_identification.md`), instead of a
single flat `mp.jwt.verify.publickey` property:

```text
tenant.registry.json
        ↓
per-tenant "jwt.publicKeyEnvVar"
        ↓
env var (PEM or JWK/JWKS)
        ↓
kid -> PublicKey map (JWTCallerPrincipalFactory)
        ↓
selected key used to verify the incoming token's signature
```

## Configuration

`tenant.registry.json` gained an optional `jwt` sub-object per tenant, alongside the
existing `mongo` one:

```json
{
  "AR": {
    "mongo": { "account": "cosmos-ar", "database": "selcOnboarding", "connectionStringEnvVar": "MONGODB_CONNECTION_STRING_AR" },
    "jwt": { "publicKeyEnvVar": "JWT_PUBLIC_KEY_AR" },
    "storages": {
      "products": {
        "account": "stselcarproducts",
        "container": "selc-d-product",
        "authentication": { "type": "MANAGED_IDENTITY", "managedIdentityClientIdEnvVar": "AZURE_CLIENT_ID_AR_PRODUCTS" }
      }
    }
  },
  "PNPG": {
    "mongo": { "account": "cosmos-pnpg", "database": "selcOnboarding", "connectionStringEnvVar": "MONGODB_CONNECTION_STRING_PNPG" },
    "jwt": { "publicKeyEnvVar": "JWT_PUBLIC_KEY_PNPG" },
    "storages": {
      "products": {
        "account": "stpnpgproducts",
        "container": "selc-d-product",
        "authentication": { "type": "MANAGED_IDENTITY", "managedIdentityClientIdEnvVar": "AZURE_CLIENT_ID_PNPG_PRODUCTS" }
      }
    }
  }
}
```

The optional `storages` map is orthogonal to JWT verification. It is shown to keep all examples aligned with
the canonical registry schema; see `Storage_identification.md`.

`JWT_PUBLIC_KEY_AR` / `JWT_PUBLIC_KEY_PNPG` are populated from Key Vault via Terraform,
reusing the existing `jwt-public-key` secret per stack (see
`infra/resources/onboarding-ms/*/onboarding.tf`).

`mp.jwt.verify.publickey` is kept as a **legacy fallback only**: it is used solely when
no tenant in the registry configures a `jwt.publicKeyEnvVar`. This keeps the other five
apps sharing `selfcare-sdk-security` (`document-ms`, `iam`, `product`, `user-ms`,
`webhook`) fully backward compatible, since none of them configure a tenant JWT
registry today.

## Key selection at verification time

Quarkus implementations may build a `kid -> PublicKey` map through
`JWTCallerPrincipalFactory` (in `selfcare-sdk-security`):
at startup:

- for every tenant with a configured `jwt.publicKeyEnvVar`, the referenced value is
  parsed as a JWK/JWKS (keyed by its own `kid`) or, if it is a plain PEM key, stored
  under a synthetic per-tenant key id `tenant:<TENANT_ID>` (e.g. `tenant:AR`);
- if no tenant contributes a key, the legacy `mp.jwt.verify.publickey` value is parsed
  the same way (JWK/JWKS by `kid`, or a single PEM ignoring `kid`).

When the resulting map holds **exactly one key** — true today, since every stack is
single-tenant (`TENANT_SUPPORTED_TENANTS=AR` or `PNPG`) — that key is used regardless of
the token's `kid`, so behavior is unchanged from before this change.

## Forward compatibility (future consolidated deployment)

When a single deployment eventually serves both tenants at once, each tenant's
`jwt.publicKeyEnvVar` **must** hold a JWK/JWKS document with a real, issuer-assigned
`kid` rather than a plain PEM key: a plain PEM's synthetic `tenant:*` kid does not match
any real token's `kid` header once more than one verification key is configured, so
signature verification would fail for every token. This constraint is enforced only in
practice (via `JWTCallerPrincipalFactory.selectPublicKey`, which requires an exact
`kid` match whenever more than one key is configured) — no code changes are needed to
migrate, only supplying JWKS-formatted key material per tenant.

### Spring implementation (`selc-commons-web`)

Spring services use `JwtService` from `selc-commons-web`. When the tenant registry is
configured, the service:

1. obtains the tenant from the request header before signature verification;
2. normalizes and validates it against the registry;
3. resolves that tenant's `jwt.publicKeyEnvVar`;
4. parses the referenced RSA PEM public key and verifies the JWT with that key;
5. propagates the authenticated tenant through `JwtAuthenticationToken` into
   `TenantContext`.

The Spring implementation currently expects an RSA public key in PEM/X.509 form and
does not use a multi-key `kid` map. If a consolidated deployment requires multiple
keys per tenant, the Spring resolver must be extended with an explicit JWKS/kid
strategy before enabling that topology; it MUST NOT silently select a first or
global key.

`JwtAuthenticationFilter` clears both `TenantContext` and the Spring security context
in a `finally` block, preventing tenant identity from leaking between reused request
threads. A missing authenticated tenant is rejected when tenant-aware security is
enabled.

When no tenant registry is configured, Spring retains the legacy global
`jwt.signingKey` fallback for backward compatibility. Once a registry is configured,
missing or invalid tenant keys fail closed and never fall back to another tenant's
key.

This keeps the per-tenant/env-var-indirection pattern consistent across JWT and Mongo
while allowing framework-specific key material and verification implementations.
