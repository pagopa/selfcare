# Internal machine-to-machine tokens

Auth issues short-lived `PAGOPA` JWTs on a **dedicated HTTPS listener**, default
port `8082`. The client library caches them locally for four minutes. Entra and
Managed Identity authentication have been removed from this flow.

No existing caller is migrated and no environment is enabled by this release.
The older caller/receiver integration in #1126 needs alignment with this contract.

## Trust boundary

The caller supplies only `tenantId`, destination `audience` and `scope`.
Its identity is **not authenticated**: the trust boundary is the Container Apps
Environment (CAE). Any workload able to reach port 8082 can request any configured
tenant/grant. Do not enable this service in a CAE containing untrusted workloads,
or expose/proxy this port through APIM, a gateway or an environment route.

Terraform adds `additionalPortMappings` with `external=false` to auth in
DEV/UAT/PROD. The existing ingress to port 8080 and login APIs remain unchanged.
`/token/service` is not registered on port 8080 and is absent from both its
generated OpenAPI and `openapi-apim.json`. The separate contract is
[`openapi-m2m.yaml`](../../apps/auth/src/main/docs/openapi-m2m.yaml).

Additional ports are TCP passthrough, so **auth terminates TLS itself**.
The listener requires a PEM certificate chain and its PEM private key; it never
falls back to HTTP. Use a certificate whose SAN matches the hostname used by
callers and a CA trusted by their JVM truststore. Do not disable hostname or
certificate verification. TLS keys are separate from tenant JWT signing keys.
Certificate/secret rotation requires a revision restart to reload the listener.

## Tenant identity, IAM and privileges

Each tenant's `m2m` configuration supplies the IAM `uid`, JWT `groups`, token
lifetime and allowed destination/scope/product mappings. The request cannot
override any of these identity/privilege fields.

For every acquisition, auth checks the existing IAM endpoint:

```text
GET <iamBaseUrl>/iam/users/<uid>/permissions/<scope>?productId=<productId>&tenantId=<tenant>
X-Tenant-Id: <tenant>
```

IAM evaluates the user's roles/permissions for that product and tenant. An
explicit `hasPermission=true` is required; denial/missing user returns 403,
and IAM failures or invalid responses return 503 without a caller token.
Configure an existing permission as the scope and associate it with the technical
user's IAM roles. This release does not provision IAM users, roles or permissions.
`iamBaseUrl` is the HTTPS base before `/iam`, selected by server configuration,
never by the caller. The route must preserve the Authorization and tenant
headers and must support IAM's permission-check operation.

IAM itself requires a JWT. Auth signs a separate 30-second bootstrap token using
the configured tenant UID/key and sends it only to IAM; it is never returned to
callers. After a successful check, auth creates the five-minute caller token.
The IAM endpoint must trust the configured tenant signing key and session audience.

Tokens contain `iss=PAGOPA`, `sub`/`uid` equal to the configured IAM user,
`name=auth`, `client_id=selfcare-m2m`, `tenant_id`, configured `groups`,
`token_use=service`, `scope`, `jti`, `iat` and `exp`. No user PII or SPID assurance
is invented. The existing security SDK grants `SUPPORT` for `PAGOPA` even if
`groups` is empty, and skips its SPID-specific tenant validation.

**Destination and scope restrict issuance, not all downstream use.** Legacy
receivers may accept the token elsewhere in the same trust domain and may not
enforce its scope. The IAM check does not change this. Configure only technical
users and groups intended for that trust level.

## Per-tenant configuration

Merge signing and M2M settings into the existing `TENANT_REGISTRY_JSON`; preserve
the other AR login/resource fields. This PNPG example uses an illustrative UID
and permission/product that must be replaced with actual IAM entries:

```json
{
  "PNPG": {
    "auth_enabled": false,
    "jwt": {
      "session": {
        "privateKeyEnvVar": "TENANT_PNPG_JWT_SESSION_PRIVATE_KEY",
        "keyIdEnvVar": "TENANT_PNPG_JWT_SESSION_KEY_ID",
        "audience": "api-pnpg.dev.selfcare.pagopa.it"
      }
    },
    "m2m": {
      "enabled": true,
      "uid": "22222222-2222-4222-8222-222222222222",
      "iamBaseUrl": "https://iam-pnpg.example.internal",
      "groups": ["SUPPORT"],
      "lifetimeSeconds": 300,
      "grants": [
        {"audience": "user-ms", "scope": "user.read", "productId": "prod-example"}
      ]
    }
  }
}
```

AR can declare a different `m2m` object and signing references. The default
lifetime is 300 seconds, permitted range 60-900. Missing/disabled `m2m` denies
issuance; an empty grants list denies all requests. Duplicate destination/scope
grants, invalid policy and missing signing keys fail initialization.

Private keys remain in secret-backed environment variables, not the registry.
There is no global-key or cross-tenant fallback. `auth_enabled` still controls
user login/session generation only; PNPG needs no OIDC/OTP mail/Mongo resources
for this exchange. Keep shared resource routing (`TENANT_SUPPORTED_TENANTS`)
limited to AR unless PNPG resources are genuinely needed.

Set `SESSION_TOKEN_TENANT_AUDIENCE_ENABLED=true` to use each tenant's
`jwt.session.audience`; otherwise the existing `SESSION_TOKEN_AUDIENCE` applies.
An absent/blank tenant audience falls back to the global value. This flag also
affects user sessions: verify AR compatibility before changing it.

## Deployment and rollback

| Environment variable in auth | Meaning |
|---|---|
| `M2M_ISSUER_ENABLED` | Global listener switch, default `false` |
| `M2M_INTERNAL_PORT` | Dedicated listener port, default `8082` |
| `M2M_TLS_CERTIFICATE_PEM` | TLS certificate chain, injected from Key Vault |
| `M2M_TLS_PRIVATE_KEY_PEM` | Matching TLS private key, injected from Key Vault |

Auth Terraform roots expose `m2m_enabled`, `m2m_tenants` and `m2m_secret_names`
locals. They default to disabled/empty. Populate `m2m_tenants` with the per-tenant
`m2m` objects above; signing references are then included for those tenants even
when login is disabled. Populate `m2m_secret_names` with environment-variable to
Key Vault secret-name mappings for both TLS values and any PNPG JWT key/`kid`.
Existing AR signing-secret bindings are retained. The module uses versionless
Key Vault references and the existing runtime secret-reader identity.
No secret values should enter Terraform state/configuration.

Before setting `m2m_enabled=true`, provision the TLS certificate/key, signing
secrets and IAM user/permissions, verify network isolation and TLS from an actual
caller, and test that port 8082 cannot be reached outside the CAE. No plan/apply,
deployment, secret provisioning or IAM data changes are performed by this PR.
Do not remove the legacy caller JWTs until consumers are migrated.

The module uses an AzAPI patch because AzureRM does not expose
`additionalPortMappings`; it reapplies after Terraform updates to the app. Other apps retain
their existing configuration (`internal_tcp_ports=null`). To clear managed extra
ports, set `internal_tcp_ports=[]` and apply before removing the opt-in entirely.
Keep an auth replica running or validate scaling for the additional TCP traffic;
do not assume the primary HTTP scale rule covers it.

Disabling the issuer or revoking the IAM permission prevents future acquisitions.
Already-issued tokens remain valid until expiry; caller cache reads do not query
IAM. Rotation must retain verification overlap for outstanding tokens.

## Client configuration and caching

Configure `selfcare.m2m.enabled=true` and
`selfcare.m2m.endpoint=https://<certificate-matching-host>:8082/token/service`.
The SDK uses normal JVM TLS verification, bounded timeouts and no redirects.
There is no Authorization header, Entra scope, identity client ID or client secret.

Cache entries are separated by tenant/destination/scope. Concurrent requests
share one acquisition. The deadline is `min(240, expires_in - 30)` seconds from
acquisition start and does not slide on reads. Failures never return stale tokens
or fall back to a static JWT. The REST-client filter remains asynchronous.

## Publication and local checks

Publish `selfcare-sdk-m2m:0.1.0` using **Release SDKs**
(`.github/workflows/release-sdk.yml`, `module=selfcare-sdk-m2m` or `all`).
Bump the version if this version was already published; never overwrite a release.
Consumer adapters from #1126 require updating before adoption.

```shell
mvn -B -f libs/selfcare-sdk-m2m/pom.xml verify
mvn -B -f apps/auth/pom.xml package -Dtest=MachineTokenServiceTest,MachineTokenTenantTest,MachineIamAuthorizerTest,MachineTokenListenerTest,SessionServiceTest,SessionServiceImplTest,TenantRegistryTest,TenantSessionKeyProviderTest,TenantResolutionFilterTest,TenantOutboundMailConfigTest
```

Tests generate temporary signing/TLS keys (using the JDK's keytool), verify HTTPS,
main-port isolation, PNPG without login, IAM failures and SDK caching. They do
not validate actual Azure reachability or deployed recipient authorization.
