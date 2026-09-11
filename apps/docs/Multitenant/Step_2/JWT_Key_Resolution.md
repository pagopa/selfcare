# JWT Verification Key Resolution

The JWT verification key used by `onboarding-ms` (and, optionally, any other app that
adopts the same pattern) is resolved through the same `TenantRegistry` already used
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
    "jwt": { "publicKeyEnvVar": "JWT_PUBLIC_KEY_AR" }
  },
  "PNPG": {
    "mongo": { "account": "cosmos-pnpg", "database": "selcOnboarding", "connectionStringEnvVar": "MONGODB_CONNECTION_STRING_PNPG" },
    "jwt": { "publicKeyEnvVar": "JWT_PUBLIC_KEY_PNPG" }
  }
}
```

`JWT_PUBLIC_KEY_AR` / `JWT_PUBLIC_KEY_PNPG` are populated from Key Vault via Terraform,
reusing the existing `jwt-public-key` secret per stack (see
`infra/resources/onboarding-ms/*/onboarding.tf`).

`mp.jwt.verify.publickey` is kept as a **legacy fallback only**: it is used solely when
no tenant in the registry configures a `jwt.publicKeyEnvVar`. This keeps the other five
apps sharing `selfcare-sdk-security` (`document-ms`, `iam`, `product`, `user-ms`,
`webhook`) fully backward compatible, since none of them configure a tenant JWT
registry today.

## Key selection at verification time

`JWTCallerPrincipalFactory` (in `selfcare-sdk-security`) builds a `kid -> PublicKey` map
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

This mirrors, on the JWT side, the same per-tenant/env-var-indirection pattern already
used for Mongo connection strings, keeping both concerns consistent and driven by the
same `tenant.registry.json`.
