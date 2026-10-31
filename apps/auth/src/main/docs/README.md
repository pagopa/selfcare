# OpenAPI specifications

This directory contains two OpenAPI specifications for the Auth microservice:

- `openapi.json`: the complete OpenAPI specification, containing all the APIs exposed by the microservice. This specification is used for the standard OpenAPI documentation and storage.
- `openapi-apim.json`: the OpenAPI specification used to configure the API exposed through Azure API Management (APIM). It contains only the APIs that are intended to be exposed through APIM.

The two files are intentionally kept separate because **not all APIs exposed by the microservice should be exposed through APIM**.

The `openapi-apim.json` file must therefore be updated whenever the set of APIs exposed through APIM changes, while `openapi.json` must continue to represent the complete API surface of the microservice.

`POST /token/service` is an opt-in machine-token exchange, separate from user
sessions. It is included only in the complete specification until its network
route and Entra grants are configured. See the
[M2M rollout guide](../../../../../libs/selfcare-sdk-m2m/README.md).
