# Machine-to-machine authentication

This release provides the opt-in machine-token issuer in `auth` and the reusable
`selfcare-sdk-m2m` client library. The library acquires a short-lived access token
using Azure Managed Identity; callers need no client secret or signing key.

No existing microservice is migrated by this release. The onboarding caller
adapters, receiver validation and cross-service HTTP integration suite remain
in the separate migration PR #1126. Existing user sessions and interservice
authentication are unchanged. Keep issuance disabled until the destination
supports the machine-token profile.

## Trust and authorization

`POST /token/service` accepts an Entra **v2 app-only access token issued for
auth**, not a SelfCare session or a token for Azure Resource Manager. Auth
validates RS256 signature, issuer, audience, expiry, issued-at, directory `tid`,
`idtyp=app`, `ver=2.0` and principal object ID `oid`.
Keys are fetched from the configured directory's Microsoft JWKS endpoint with
bounded network timeouts and cached by jose4j.

An exact grant associates that principal with a service name, SelfCare tenant,
destination and permission. An Entra directory is not an AR/PNPG tenant.
The requester cannot choose its service identity or obtain a grant through
`X-Tenant-Id`. Missing grants deny access.

Auth signs a JWT with `iss=SELFCARE-M2M`, `token_use=service`, `sub`, `uid`,
`tenant_id`, `aud`, `scope`, `jti`, `iat` and `exp`. Lifetime defaults to five
minutes and must be 60-900 seconds. Existing tenant signing keys are reused;
there is no change to user-token semantics or the `PAGOPA` support role.

Before a destination can accept these tokens, it must validate its tenant's
signature, exact audience and permission, machine profile, explicit tenant,
lifetime and tenant-header consistency, and enforce authorization for the
requested operation. Receiver integration is not included in this release;
accepting the issuer alone is insufficient.

The client caches separately by tenant, audience and permission (the identity
is fixed per client instance), coalesces concurrent acquisitions and reuses a
token for at most **four minutes**, measured from the start of acquisition.
Cache reads do not extend this deadline. With the default five-minute JWT this
leaves a one-minute safety margin. Shorter-lived tokens are renewed no later
than 30 seconds before expiry. Renewal happens on the next request, not on a
background timer. The reactive request filter suspends/resumes instead of
blocking an event-loop thread. Acquisition failures fail the call; they never
fall back to the static JWT or reuse a token inside its renewal window.
The Azure SDK manages the separate Entra access token lifetime.

## Deployment prerequisites

1. Publish `selfcare-sdk-m2m:0.1.0` before deploying its consumers.
2. Register an Entra application representing the token API in each intended
   trust boundary. Set `api.requestedAccessTokenVersion` to `2` and add the
   `idtyp` optional **access-token** claim. The Entra audience is the API
   application's client ID; its scope for acquisition is
   `api://<application-client-id>/.default` when using that application ID URI.
3. Use each caller's system-assigned Managed Identity or a dedicated
   user-assigned identity. Do not authorize the shared Container App
   Environment identity. Grant API access to the chosen identities under your
   Entra policy; configure auth's allowlist with their **principal object IDs**,
   not their client IDs. No Azure resource changes or role assignments are
   performed by this code.
4. Make auth's `/token/service` reachable from callers over HTTPS. The complete
   auth OpenAPI includes it; `openapi-apim.json` is intentionally unchanged.
   If APIM is the selected route, publish an explicitly restricted operation
   preserving the Entra Authorization header; do not use an external-API policy
   that replaces it with a SelfCare JWT. Verify the actual network route before
   enabling callers.
5. Auth must already support the requested SelfCare tenant and have its signing
   key. The destination must have the corresponding verification key and
   machine-token validation. Preserve any additional destination authentication
   requirements, such as function keys.

### Publishing the SDK

After merge, run **Release SDKs** (`.github/workflows/release-sdk.yml`) with
`module=selfcare-sdk-m2m` on `main`. The workflow also includes M2M when
`module=all`. It builds/installs each selected module and its dependencies, then
publishes only the selected module to GitHub Packages.

This release adds manual selection, not a new automatic push trigger. Bump the
SDK version before publishing changed artifacts; do not republish an existing
release version.

Configure these values through the environment's Terraform app settings; no
environment is enabled by default:

| Component | Environment variable | Value |
|---|---|---|
| auth | `M2M_ISSUER_ENABLED` | `true` after prerequisites |
| auth | `M2M_ENTRA_TENANT_ID` | Entra directory UUID |
| auth | `M2M_ENTRA_AUDIENCE` | Token API application's client ID |
| auth | `M2M_TOKEN_LIFETIME` | `PT5M` (default) |
| auth | `M2M_GRANTS_JSON` | Explicit grants, default `[]` denies all |

The SDK is configured by its consumer through `selfcare.m2m.enabled`,
`selfcare.m2m.endpoint`, `selfcare.m2m.entra-scope` and the optional
`selfcare.m2m.managed-identity-client-id`. Caller-specific environment mappings,
REST-client filter registration and receiver activation belong to the later
migration, not this release.

Example grant structure (replace the illustrative object ID with the actual
managed identity principal ID):

```json
[
  {
    "objectId": "22222222-2222-4222-8222-222222222222",
    "serviceId": "onboarding-cdc",
    "tenantId": "AR",
    "audience": "onboarding-functions",
    "scope": "onboarding-functions.invoke"
  }
]
```

Add separate grants for each caller and allowed SelfCare tenant. Grants for
destinations without M2M validation must not be added.

## Rollout and rollback

Publish the SDK and deploy auth with `M2M_ISSUER_ENABLED=false` (the default).
This can be done independently of the onboarding migration and does not replace
any existing token. Do not remove legacy secrets or enable grants for unmigrated
destinations.

In the later migration, deploy receiver validation before enabling issuance and
callers. Verify successful calls, rejection for a different tenant/destination,
expiration, and failure when auth is unavailable in DEV before wider rollout.
Caller rollback must be explicit, never an automatic fallback on token errors.

Removing a grant or disabling an identity prevents future issuance; already
issued offline-validated JWTs remain valid until expiry. Signing-key rotation
must preserve verification overlap for the outstanding lifetime.

## Local checks

```shell
mvn -f libs/selfcare-sdk-m2m/pom.xml install
mvn -f apps/auth/pom.xml test -Dtest=MachineIdentityVerifierTest,MachineTokenServiceTest,MachineTokenControllerTest
```

Tests use generated RSA keys, a local token server or mocked credentials; they
never contact Entra or require real credentials. Maven still needs access to
the repository's private package dependencies.

The cross-service HTTP suite and its `m2m-integration` profile are deliberately
excluded: they require the onboarding changes from PR #1126. This release's
checks cover issuer validation/signing, grants, HTTP responses and the SDK's
cache/filter behavior, not a complete deployed caller-to-receiver flow. An Azure
smoke test is still required before activation.
