# Machine-to-machine authentication

Opt-in migration for `onboarding-ms` / `onboarding-cdc` calling
`onboarding-functions`. User sessions continue to be propagated unchanged.
Background calls acquire a short-lived access token from `auth`, authenticating
with Azure Managed Identity. No client secret or signing key is needed by these
callers.

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

The functions validate their tenant's signature, exact audience
`onboarding-functions`, permission `onboarding-functions.invoke`, machine
profile, explicit tenant, lifetime and tenant-header consistency before running
the operation. This initial permission grants invocation of the protected
functions, not per-operation authorization.

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
   key. The functions must have the corresponding verification key. Existing
   function-key authentication (`x-functions-key`) remains required.

Configure these values through the environment's Terraform app settings; no
environment is enabled by default:

| Component | Environment variable | Value |
|---|---|---|
| auth | `M2M_ISSUER_ENABLED` | `true` after prerequisites |
| auth | `M2M_ENTRA_TENANT_ID` | Entra directory UUID |
| auth | `M2M_ENTRA_AUDIENCE` | Token API application's client ID |
| auth | `M2M_TOKEN_LIFETIME` | `PT5M` (default) |
| auth | `M2M_GRANTS_JSON` | Explicit grants, default `[]` denies all |
| onboarding-functions | `M2M_VALIDATION_ENABLED` | `true` before enabling callers |
| onboarding-ms / onboarding-cdc | `M2M_CLIENT_ENABLED` | `true` to replace static background tokens |
| onboarding-ms / onboarding-cdc | `M2M_TOKEN_ENDPOINT` | Full HTTPS URL ending in `/token/service` |
| onboarding-ms / onboarding-cdc | `M2M_ENTRA_SCOPE` | API application ID URI plus `/.default` |
| onboarding-ms / onboarding-cdc | `M2M_MANAGED_IDENTITY_CLIENT_ID` | Omit for system-assigned; dedicated user-assigned client ID otherwise |

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

Deploy disabled, then enable validation in functions and issuance in auth.
Enable one caller in DEV and verify successful calls, rejection for a different
tenant/destination, expiration, and failure when auth is unavailable. Then
enable other callers/environments.

`M2M_CLIENT_ENABLED=false` explicitly restores legacy behavior. This is an
operator-controlled rollback, not automatic fallback. Keep the legacy secret
reference only for the agreed rollback period.

Remove `JWT_BEARER_TOKEN` from migrated callers after the rollout. Do **not**
delete the shared Key Vault secret globally: `institution-send-mail-scheduler`
and non-migrated deployments still use it. Outbound calls from
`onboarding-functions` continue their existing local signing; migrating those
and the scheduler requires M2M authorization in their other destinations.

Removing a grant or disabling an identity prevents future issuance; already
issued offline-validated JWTs remain valid until expiry. Signing-key rotation
must preserve verification overlap for the outstanding lifetime.

## Local checks

```shell
mvn -f libs/selfcare-sdk-m2m/pom.xml install
mvn -f apps/auth/pom.xml test -Dtest=MachineIdentityVerifierTest,MachineTokenServiceTest,MachineTokenControllerTest
mvn -f apps/onboarding-ms/pom.xml test -Dtest=TenantHeaderClientRequestFilterTest,OnboardingFunctionsClientTenantPropagationConfigTest
mvn -f apps/onboarding-cdc/pom.xml test -Dtest=AuthenticationPropagationHeadersFactoryTest,TenantHeaderClientRequestFilterTest,OnboardingFunctionsClientTenantPropagationConfigTest
mvn -f apps/onboarding-functions/pom.xml test -Dtest=HttpJwtValidatorTest
```

Tests use generated RSA keys, a local token server or mocked credentials; they
never contact Entra or require real credentials. Maven still needs access to
the repository's private package dependencies.

### Cross-service HTTP integration suite

From the repository root, with Java 17 and Maven configured for private packages:

```shell
bash apps/onboarding-functions/src/m2m-test/run.sh
```

The launcher builds the current SDK and a small `m2m-contract` classifier containing
the **compiled production validator** from the current functions sources. It then
runs `MachineTokenFlowIT` inside auth, using its real HTTP endpoint, grant checks,
tenant key provider and JWT signing. RSA keys are generated for each test boot;
only the Entra identity-verification boundary is mocked.

Each caller runs in a separate Maven/Quarkus test process, using its own dependency
versions, real generated REST client, filters and SDK cache. A shared test-only
credential source replaces Managed Identity; no production authentication bypass
or HTTP endpoint override is added. All test HTTP servers bind to loopback with
dynamic ports. No service images, Docker, `latest` tags or deployed services are
needed. The launcher installs branch artifacts locally; it does not publish them.

Both AR and PNPG flows cover five-minute JWTs, separate tenant cache entries,
reuse at 239 seconds, renewal at 240 seconds, failed renewal without stale/static
fallback, and recovery. The clock advances only inside the client, without sleeps.
Additional scenarios reject invalid identities, missing grants, wrong audience,
scope or tenant, altered signatures and expired tokens.

The receiver is an HTTP adapter around the real `HttpJwtValidator`, **not the
Azure Functions host**. This suite checks the authentication protocol, not Durable
Functions execution, Mongo change streams, APIM, TLS or real Entra/Managed Identity
connectivity. Keep the existing Cucumber business-flow suites and an Azure smoke
test for those boundaries.

Sources shared by the suite live in `apps/onboarding-functions/src/m2m-test/`;
caller-specific tests live in each caller's `src/m2m-test/java/`. They are compiled
only with `-Pm2m-integration` and selected explicitly. Auth and caller builds go
to separate `target/m2m/` directories, with reports in `target/m2m/surefire-reports`,
leaving normal test runs unchanged. Auth's report contains four scenarios; each
caller's report contains two. Child-process logs are in
`apps/auth/target/m2m-onboarding-*.log`.
