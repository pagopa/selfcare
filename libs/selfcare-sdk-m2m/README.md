# Internal machine-to-machine tokens

Auth issues short-lived `PAGOPA` JWTs on a **dedicated internal listener**, default
port `8082`, using HTTPS by default. The client library caches them locally for four minutes. Entra and
Managed Identity authentication have been removed from this flow.

No existing caller is migrated. DEV/UAT/PROD Terraform enables the internal listener in
explicit plaintext HTTP mode; tenant issuance remains denied until real IAM
identities and signing keys are configured. No environment has been deployed by this change.
The older caller/receiver integration in #1126 needs alignment with this contract.

## Trust boundary

The caller supplies only `tenantId`.
Its identity is **not authenticated**: the trust boundary is the Container Apps
Environment (CAE). Any workload able to reach port 8082 can request any configured
tenant identity. Do not enable this service in a CAE containing untrusted workloads,
or expose/proxy this port through APIM, a gateway or an environment route.

Terraform adds `additionalPortMappings` with `external=false` to auth in
DEV/UAT/PROD. The existing ingress to port 8080 and login APIs remain unchanged.
`/token/service` is not registered on port 8080 and is absent from both its
generated OpenAPI and `openapi-apim.json`. The separate contract is
[`openapi-m2m.yaml`](../../apps/auth/src/main/docs/openapi-m2m.yaml).

Additional ports are TCP passthrough, so **auth terminates TLS itself when enabled**.
With the default `M2M_TLS_ENABLED=true`, the listener requires a PEM certificate
chain and its PEM private key; missing or invalid secrets never trigger an HTTP
fallback. Use a certificate whose SAN matches the hostname used by
callers and a CA trusted by their JVM truststore. Do not disable hostname or
certificate verification. TLS keys are separate from tenant JWT signing keys.
Certificate/secret rotation requires a revision restart to reload the listener.

DEV/UAT/PROD explicitly set `M2M_TLS_ENABLED=false`. Callers must also explicitly enable
`selfcare.m2m.allow-insecure-http=true`; simply configuring an HTTP URL is rejected
by the SDK. No TLS certificate is required for this mode, and CAE peer-to-peer
encryption settings are not changed. The application connection is plaintext;
where platform peer encryption is disabled, tokens also travel unencrypted across
the internal network. CAE-only reachability is not encryption or per-tenant isolation.
The existing public HTTP ingress/TLS configuration is unchanged.

## Tenant identity, IAM and privileges

Each tenant's `m2m` configuration supplies an IAM `uidEnvVar` reference, JWT
`groups` and token lifetime. The request cannot
override any of these identity/privilege fields.

Auth resolves the UID from that environment variable using the same configuration
mechanism as tenant signing-key references. Enabled tenants fail startup if the
reference/value is missing, blank or the value is not a canonical UUID. There is
no literal `m2m.uid`, global-UID or cross-tenant fallback. A single resolved UID is
used for the session JWT in each acquisition; invalid configuration
errors do not include its value. Secret updates require restarting the revision
to refresh its environment. The UID still appears in the signed JWT claims and
secret-backed configuration does not encrypt JWT contents.

Auth issues a five-minute session token without contacting IAM or checking
whether the UID exists or has permissions. No bootstrap token is created.
The receiving service must authorize the requested operation using the technical
user's roles/permissions, querying IAM where required. This release does not
provision IAM users or add IAM checks to recipients.

Tokens contain `iss=PAGOPA`, `sub`/`uid` equal to the configured IAM user,
`name=auth`, `client_id=selfcare-m2m`, `tenant_id`, configured `groups`,
`token_use=service`, `jti`, `iat` and `exp`. No scope, user PII or SPID assurance
is invented. The existing security SDK grants `SUPPORT` for `PAGOPA` even if
`groups` is empty, and skips its SPID-specific tenant validation.

**A session token is not proof of an IAM permission check.** Legacy receivers
may accept `PAGOPA`/`SUPPORT` tokens without IAM. Configured groups are not
retrieved from IAM. Configure only technical users and groups intended for this
trust level, and verify recipient authorization before rollout.

## Per-tenant configuration

Auth's `M2M_ALLOWED_TENANTS` (`auth.m2m.allowed-tenants`) is a comma-separated
allowlist, defaulting to `AR,PNPG`. Tenant IDs are case-sensitive and must match
`[A-Za-z0-9_-]{1,64}`. A tenant must also be present in `TENANT_REGISTRY_JSON`
with `m2m.enabled` and signing credentials; listing it alone does not
enable issuance. An enabled M2M tenant outside the allowlist or an invalid
allowlist causes startup to fail.

Merge signing and M2M settings into the existing `TENANT_REGISTRY_JSON`; preserve
the other AR login/resource fields. This PNPG example references the agreed UID
environment variable:

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
      "uidEnvVar": "TENANT_PNPG_M2M_IAM_UID",
      "groups": [],
      "lifetimeSeconds": 300
    }
  }
}
```

AR can declare a different `m2m` object and signing references. The default
lifetime is 300 seconds, permitted range 60-900. Missing/disabled `m2m` denies
issuance. Invalid lifetime/groups and missing signing keys fail initialization.
`iamBaseUrl` and `grants` are no longer used and should be removed from existing
M2M configuration.

Each environment maps these versionless references from its own Key Vault:

| Tenant | `m2m.uidEnvVar` | Key Vault secret name |
|---|---|---|
| AR | `TENANT_AR_M2M_IAM_UID` | `auth-m2m-iam-uid-ar` |
| PNPG | `TENANT_PNPG_M2M_IAM_UID` | `auth-m2m-iam-uid-pnpg` |

The vaults are `selc-d-kv` (DEV), `selc-u-kv` (UAT) and `selc-p-kv` (PROD).
Secret names are identical; values must be populated for the intended IAM identity
in each environment and are not assumed to be shared.
Populate each secret with the corresponding technical IAM user's UUID outside
Terraform. The secret value is never embedded in `TENANT_REGISTRY_JSON`, Terraform
variables or the repository. The Container App's existing managed identity reads
the secrets at runtime; it is not used to authenticate M2M callers.

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
| `M2M_ALLOWED_TENANTS` | Comma-separated M2M tenant allowlist, default `AR,PNPG` |
| `M2M_TLS_ENABLED` | Default `true`; explicit `false` permits plaintext HTTP |
| `M2M_TLS_CERTIFICATE_PEM` | TLS certificate chain, injected from Key Vault |
| `M2M_TLS_PRIVATE_KEY_PEM` | Matching TLS private key, injected from Key Vault |

Auth Terraform roots expose `m2m_enabled`, `m2m_tenants` and `m2m_secret_names`
locals. All three environments enable only the HTTP listener and declare disabled AR/PNPG policies
with UID references and their secret bindings, so both tenants return 403 for
valid requests. Create both UID secrets in each target vault before applying its configuration:
Azure must resolve the secret bindings even while tenant issuance is disabled.
Complete `m2m_tenants` with the per-tenant
`m2m` objects above; signing references are then included for those tenants even
when login is disabled. Populate `m2m_secret_names` with environment-variable to
Key Vault secret-name mappings for both TLS values and any PNPG JWT key/`kid`.
Existing AR signing-secret bindings are retained. The module uses versionless
Key Vault references and the existing runtime secret-reader identity.
The unused data sources that previously listed and fetched every secret value
have been removed from the Container App module. This prevents new UID values
from being fetched into Terraform state by this module; it does not purge secret
values from historical state versions.

Before enabling tenant issuance, provision signing secrets and the technical IAM user/permissions
and, for HTTPS deployments, the TLS certificate/key. Verify the configured
transport from an actual caller and that port 8082 cannot be reached outside the CAE. No plan/apply,
deployment, secret provisioning or IAM data changes are performed by this PR.
Do not remove the legacy caller JWTs until consumers are migrated.
Deploy an auth image supporting `M2M_TLS_ENABLED` before enabling the HTTP
configuration; an older TLS-only image still requires certificate/key secrets.

The module uses an AzAPI patch because AzureRM does not expose
`additionalPortMappings`; it reapplies after Terraform updates to the app. Other apps retain
their existing configuration (`internal_tcp_ports=null`). To clear managed extra
ports, set `internal_tcp_ports=[]` and apply before removing the opt-in entirely.
All environments set one minimum auth replica so the additional TCP listener does
not depend on primary HTTP traffic to wake the app. This increases DEV's baseline
running cost; UAT/PROD already inherited a minimum of one. Existing maximum
replicas, cron scaling and CPU/memory settings are preserved.

Disabling the issuer or tenant prevents future acquisitions. IAM revocation does
not prevent issuance: its effect depends on downstream permission checks and
their caches. Already-issued tokens remain valid until expiry. Rotation must
retain verification overlap for outstanding tokens.

## Client configuration and caching

Configure `selfcare.m2m.enabled=true` and
`selfcare.m2m.endpoint=https://<certificate-matching-host>:8082/token/service`.
The SDK's `selfcare.m2m.allowed-tenants` also defaults to `AR,PNPG`; configure it
with the tenant IDs the client needs, and include those IDs in auth's allowlist.
The client list is not an authorization boundary: auth independently enforces
its own list and per-tenant enablement.
The SDK uses normal JVM TLS verification, bounded timeouts and no redirects.
There is no Authorization header, Entra scope, identity client ID or client secret.

The request is `{"tenantId":"AR"}` and the response contains only `access_token`,
`token_type` and `expires_in`. Removed `audience`, `scope` and `productId` request
fields are rejected with 400; update clients together with auth.
Use `MachineTokenClient.token(tenantId)`; the old three-argument method and
`selfcare.m2m.audience` / `selfcare.m2m.scope` settings have been removed.

For the explicit HTTP mode, configure (DEV example):

```properties
selfcare.m2m.enabled=true
selfcare.m2m.endpoint=http://selc-d-auth-ms-ca:8082/token/service
selfcare.m2m.allow-insecure-http=true
```

Use the endpoint and CAE for the target environment:

| Environment | Endpoint | Auth CAE |
|---|---|---|
| DEV | `http://selc-d-auth-ms-ca:8082/token/service` | `selc-d-cae-002` |
| UAT | `http://selc-u-auth-ms-ca:8082/token/service` | `selc-u-cae-002` |
| PROD | `http://selc-p-auth-ms-ca:8082/token/service` | `selc-p-cae-002` |

Each app-name endpoint is for callers in that auth CAE only.
It can request either tenant once policies are configured; it does not make
the listener reachable from the separate PNPG CAE. Tenant JWT signing keys are
required even when caller transport is HTTP; auth makes no IAM call for issuance.
No consumer deployment is changed automatically by these settings.

Cache entries are separated by tenant and shared across destinations. Concurrent requests
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
mvn -B -f apps/auth/pom.xml package -Dtest=MachineTokenServiceTest,MachineTokenTenantTest,MachineTokenHttpTest,MachineTokenListenerTest,SessionServiceTest,SessionServiceImplTest,TenantRegistryTest,TenantSessionKeyProviderTest,TenantResolutionFilterTest,TenantOutboundMailConfigTest
```

Tests generate temporary signing/TLS keys (using the JDK's keytool), verify HTTPS
defaults, explicit HTTP opt-in without TLS secrets, main-port isolation,
PNPG without login, secret-backed tenant identities, missing/invalid UID rejection,
tenant-only issuance without IAM/grants, removed-field rejection and SDK caching. They do
not validate actual Azure reachability or deployed recipient authorization.
