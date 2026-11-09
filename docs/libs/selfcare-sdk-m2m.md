# selfcare-sdk-m2m

> Quarkus client to obtain short-lived SelfCare machine-to-machine (service) JWTs from the `auth` service using Azure Managed Identity, and attach them to outgoing REST client calls.

> [!WARNING]
> This library is **not on the documented branch**. `libs/selfcare-sdk-m2m` has no tracked files here (only local build
> output may exist under `target/`); it exists on branch `feature/SELC-9615` (commit `db1c36bd5`). No app on this
> branch depends on it. Everything below is read from that commit and may change before it is merged.

| | |
|---|---|
| Path | `libs/selfcare-sdk-m2m` (branch `feature/SELC-9615`) |
| Artifact | `it.pagopa.selfcare:selfcare-sdk-m2m:0.1.0` (parent [`selfcare-sdk-pom:1.0.0`](selfcare-sdk-pom.md)) |
| Stack | Quarkus (CDI, `quarkus-rest-client`), `azure-identity` 1.18.4, Jackson |
| Used by | none on this branch |

## What it provides

Package `it.pagopa.selfcare.m2m`:

| Class | Purpose |
|---|---|
| `MachineTokenClient` | `@ApplicationScoped`. Gets an Entra ID app-only token via Managed Identity for `selfcare.m2m.entra-scope` (must end in `/.default`), calls the `auth` token endpoint (`selfcare.m2m.endpoint`), validates the response (scope match, lifetime 60–900 s) and caches the service token (max 4 minutes, refreshed 30 s before expiry). `token(tenant, audience, scope)` returns `CompletionStage<String>`. |
| `MachineTokenRequestFilter` | REST client request filter (`Priorities.USER + 100`) adding the service token; uses `selfcare.m2m.audience` and `selfcare.m2m.scope`. Register it only on the intended downstream client, after the tenant header filter. |

Configuration properties: `selfcare.m2m.enabled` (default `false`), `selfcare.m2m.endpoint`, `selfcare.m2m.entra-scope`,
`selfcare.m2m.managed-identity-client-id`, `selfcare.m2m.audience`, `selfcare.m2m.scope`.

On the feature branch the intended flow is: `onboarding-ms` / `onboarding-cdc` → `auth` `POST /token/service` →
call `onboarding-functions` with the service JWT.

## Usage

```xml
<dependency>
  <groupId>it.pagopa.selfcare</groupId>
  <artifactId>selfcare-sdk-m2m</artifactId>
  <version>0.1.0</version>
</dependency>
```

## Build and release

Not part of `libs/pom.xml` and no release workflow on this branch.

## References

- `libs/selfcare-sdk-m2m/README.md` on branch `feature/SELC-9615`
- [auth app](../apps/auth.md), [selfcare-sdk-security](selfcare-sdk-security.md)
