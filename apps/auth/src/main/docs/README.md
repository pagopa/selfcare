# OpenAPI specifications

This directory separates the Auth microservice's API contracts by listener:

- `openapi.json` / `openapi.yaml`: generated specifications for the primary HTTP listener (port 8080).
- `openapi-apim.json`: the OpenAPI specification used to configure the API exposed through Azure API Management (APIM). It contains only the APIs that are intended to be exposed through APIM.
- `openapi-m2m.yaml`: hand-maintained contract for the dedicated internal listener (port 8082, HTTPS by default; explicit HTTP opt-in in DEV). Never import it into APIM.

The files are intentionally kept separate because **not all APIs exposed by the microservice should be exposed through APIM**.

Update `openapi-apim.json` when the APIM surface changes, regenerate the primary
specifications when its APIs change, and update the internal contract separately.

`POST /token/service` exists only on the opt-in internal listener.
DEV enables plaintext HTTP explicitly, without enabling peer-to-peer encryption.
Its tenant policies remain unconfigured, so neither tenant can obtain tokens yet.
It requires no caller credentials: CAE network isolation is the trust boundary.
An explicit tenant grant and successful IAM permission check are required.
It emits `PAGOPA` tokens with the tenant's configured IAM UID, session key/audience
and a default five-minute lifetime. Grant destination/scope restrict issuance,
not downstream access in legacy receivers; the legacy security SDK grants
`SUPPORT` for `PAGOPA` and skips SPID-specific tenant validation. See the
[M2M rollout guide](../../../../../libs/selfcare-sdk-m2m/README.md).
