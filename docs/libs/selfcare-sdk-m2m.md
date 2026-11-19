# selfcare-sdk-m2m

> Quarkus client to obtain short-lived technical session JWTs from auth's CAE-only listener and attach them to outgoing REST client calls.

| | |
|---|---|
| Path | `libs/selfcare-sdk-m2m` |
| Artifact | `it.pagopa.selfcare:selfcare-sdk-m2m:0.1.0` (parent [`selfcare-sdk-pom:1.0.0`](selfcare-sdk-pom.md)) |
| Stack | Quarkus (CDI, `quarkus-rest-client`), Jackson |
| Used by | none on this branch |

## What it provides

Package `it.pagopa.selfcare.m2m`:

| Class | Purpose |
|---|---|
| `MachineTokenClient` | `@ApplicationScoped`. Calls auth's internal `/token/service` endpoint with only `tenantId`, validates the response (Bearer token, lifetime 60–900 s) and caches the session per tenant for `min(240, expires_in - 30)` seconds from acquisition start. `token(tenant)` returns `CompletionStage<String>`. Concurrent requests share one acquisition; no stale/static fallback. |
| `MachineTokenRequestFilter` | Asynchronous REST client filter (`Priorities.USER + 100`) adding the session token selected by `X-Tenant-Id`; preserves an existing Authorization header. Register it only on the intended downstream client, after the tenant header filter. |

Configuration properties: `selfcare.m2m.enabled` (default `false`), `selfcare.m2m.endpoint`,
`selfcare.m2m.allowed-tenants` (default `AR,PNPG`) and
`selfcare.m2m.allow-insecure-http` (default `false`). HTTPS uses normal JVM certificate
and hostname verification; HTTP requires explicit opt-in in auth and the SDK.

No caller credentials or Entra tokens are used: any workload reaching the listener
can obtain any enabled tenant's identity. Auth signs a short-lived `PAGOPA` session
for the configured technical UID without grant or IAM checks. Recipients must
authorize operations, using IAM where required; this library does not add those
checks. Legacy recipients may assign `SUPPORT` to `PAGOPA` without IAM.
The listener must remain CAE-only and must not be published through APIM.

## Usage

```xml
<dependency>
  <groupId>it.pagopa.selfcare</groupId>
  <artifactId>selfcare-sdk-m2m</artifactId>
  <version>0.1.0</version>
</dependency>
```

## Build and release

Registered in `libs/pom.xml`. Validate with
`mvn -B -f libs/selfcare-sdk-m2m/pom.xml verify`; publish through
`.github/workflows/release-sdk.yml` with `module=selfcare-sdk-m2m`.
Never overwrite an already published version.

## References

- [Configuration, transport and rollout guide](../../libs/selfcare-sdk-m2m/README.md)
- [auth app](../apps/auth.md), [selfcare-sdk-security](selfcare-sdk-security.md)
