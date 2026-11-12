# Machine-to-machine authentication

This release provides the opt-in machine-token issuer in `auth` and the reusable
`selfcare-sdk-m2m` client library. The library acquires a short-lived access token
using Azure Managed Identity; callers need no client secret or signing key.

No existing caller is migrated by this release. Auth issues technical tokens
using the existing `PAGOPA` issuer, rather than APIM's `SPID` issuer.
Receivers already supporting `PAGOPA` do not need a new issuer or machine-profile
validator. Existing user sessions remain unchanged.
Keep issuance disabled until the destination's signing-key trust, session
audience and authorization behavior have been verified.

The earlier `SELFCARE-M2M` receiver changes and cross-service suite in PR #1126
target a different profile and must be adapted before that PR is used with this
issuer. They are not required to validate these legacy-compatible tokens.

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

Auth signs an RS256 JWT with `iss=PAGOPA`, `name=auth`, and `sub`, `uid` and
`client_id` set to the granted service identity, never to a user supplied by the
caller. `tenant_id`, `iat` and `exp` follow the existing session convention.
Additional `token_use=service`, `scope` and `jti` claims identify technical use
but do not require changes in legacy receivers. No fiscal code, e-mail, SPID
authentication level or explicit role claim is asserted.

**Issuer semantics:** the existing `selfcare-sdk-security` assigns the
`SUPPORT` role to `PAGOPA` tokens even without a `groups` claim. Its identity
augmentor also skips the tenant validation performed for `SPID`. Keeping
`tenant_id` in the token does not restore those SPID checks. Only authorize
machine identities intended to receive this support-level trust, and verify
tenant isolation and authorization in the actual receivers before activation.

Lifetime defaults to five minutes and must be 60-900 seconds. The private key
and `kid` come from the same tenant signing-key provider as user sessions.
The JWT `aud` uses the existing session audience resolver:
`SESSION_TOKEN_AUDIENCE`, or `jwt.session.audience` in `TENANT_REGISTRY_JSON`
when `SESSION_TOKEN_TENANT_AUDIENCE_ENABLED=true` (with the existing global
fallback). Neither session lifetime nor user/SAML token issuance is changed.

**Authorization limitation:** the request/grant `audience` is a destination
identifier used only to authorize issuance; it is not the emitted JWT `aud`.
The request's `scope` also gates issuance, but legacy receivers do not enforce
these M2M scopes. A token may authenticate to other services trusting the same
key, issuer and session audience. This is a shared legacy trust model, not
cryptographic confinement to the selected destination. Endpoint-specific user
or relationship checks still apply and may reject the technical identity.
Do not grant issuance to identities that must not have this broader trust.

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
   key. The destination must trust the corresponding public key/certificate
   (and `kid`, where enforced), accept `PAGOPA` and the configured session
   audience, and allow the technical identity on the required operation.
   Preserve additional authentication requirements such as function keys.

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
REST-client filter registration and receiver compatibility checks belong to
the later migration, not this release. No `SELFCARE-M2M` receiver activation
is needed.

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

Add separate grants for each caller and allowed SelfCare tenant. Authorize a
destination only after verifying its existing PAGOPA validation and the broader
trust described above; grant scope alone does not restrict downstream access.

## Rollout and rollback

Publish the SDK and deploy auth with `M2M_ISSUER_ENABLED=false` (the default).
This can be done independently of the onboarding migration and does not replace
any existing token. Do not remove legacy secrets or enable grants before
verifying receiver compatibility.

In the later caller migration, verify successful calls, tenant isolation,
rejection of unauthorized issuance requests, expiration and failure when auth
is unavailable in DEV before wider rollout. Do not expect legacy receivers to
reject a token solely because it was requested for another destination.
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
excluded: the suite in PR #1126 needs adaptation to the PAGOPA profile.
This release's checks cover issuer validation/signing, grants, HTTP responses,
SmallRye validation using the legacy issuer/audience/required-claim rules, and
the SDK's cache/filter behavior, not every deployed receiver or a complete
caller-to-receiver flow. An Azure smoke test is required before activation.
