# SelfCare Security SDK

This SDK provides custom security features for Quarkus applications, specifically enhancing JWT processing to support multiple issuers and conditional role assignment.

## Key Features
This library replaces Quarkus's default JWT validation mechanism to enforce custom business logic regarding token issuers and associated user roles:

Multi-Issuer Validation: It enforces that any incoming JSON Web Token (JWT) must be issued by one of the following recognized authorities: SPID or PAGOPA.

Custom Public Key Verification: Both JWT will use the same pre-configured shared public key for signature verification, simplifying trust management.

Conditional Role Assignment: It conditionally augments the authenticated user's Security Identity by adding the SUPPORT role if the token's issuer is PAGOPA.

Tenant Consistency Validation: For tokens issued by SPID, the validated
`tenant_id` claim is reconciled with the trusted `X-Tenant-Id` request header.
When the claim is absent, the effective tenant is `PNPG`; a missing, unknown,
or mismatching header is rejected.

Logging Context Enrichment: `LoggingContextFilter` copies a configurable set of
JWT claims into the logging context (MDC) of every REST request, so that every
log line written while serving the request reports who performed it.

## Configuration and Usage

The core functionality is implemented within the custom

```JWTCallerPrincipalFactory```, which utilizes CDI

```@Alternative``` and ```@Priority``` to override the default Quarkus behavior.

### Adding the Dependency to your POM
To use the SDK, add the following dependency to your consuming project's pom.xml file.

```
<dependency>
    <groupId>it.pagopa.selfcare</groupId>
    <artifactId>selfcare-sdk-security</artifactId>
    <version>0.0.1</version>
</dependency>
```
### Application Properties Setup
The SDK requires a single mandatory configuration property: the public key used to verify the JWT signature.

Add the following property to your src/main/resources/application.properties file:

```mp.jwt.verify.publickey=${JWT-PUBLIC-KEY}```

### How it Works (Technical Details)
The custom logic is primarily executed within the overridden ```parse``` method of ```JWTCallerPrincipalFactory```.

- Issuer Extraction: The token's payload is manually parsed to extract the ```iss``` claim before standard validation begins.
- Issuer Check: The extracted issuer is checked against the internal set of valid issuers (```SPID```, ```PAGOPA```). If the issuer is not allowed, a ```ParseException``` is thrown, failing the authentication.
- Key Setup: The shared public key, injected during the constructor phase, is set on the ```JWTAuthContextInfo``` used for the standard validation process.
- Security Identity Augmentation: The custom logic ensures that tokens issued by ```PAGOPA``` result in a ```SecurityIdentity``` that includes the ```SUPPORT``` role, enabling access to specific privileged endpoints.
  - (Note: While the provided class is a ```JWTCallerPrincipalFactory```, the role assignment is typically handled by a subsequent ```SecurityIdentityAugmentor``` in a complete flow. However, the custom factory sets the foundation by ensuring only valid issuers pass the initial check.)
- Tenant Validation: For a verified `SPID` token, the SDK exposes the effective
  tenant as the `jwt.tenant` security identity attribute and validates it
  against the `X-Tenant-Id` header after authentication.

### Using the SDK in a non tenant-aware application

Since `0.5.0` the jar ships a Jandex index, so every bean of the SDK is discovered
automatically. An application that is not tenant-aware yet can exclude
`selfcare-sdk-tenant` from the dependency and keep only `LoggingContextFilter`:

```
quarkus.arc.exclude-types=it.pagopa.selfcare.security.JWTCallerPrincipalFactory,it.pagopa.selfcare.security.JWTSecurityIdentityAugmentor
selfcare.security.tenant-validation.enabled=false
```

`JwtTenantValidationFilter` is a JAX-RS provider: RESTEasy Reactive registers it
even when excluded from CDI, so it is disabled through the build-time property
`selfcare.security.tenant-validation.enabled` (default `true`).

### Logging Context (MDC)

`LoggingContextFilter` is a RESTEasy Reactive request/response filter that is
registered automatically (the consuming application must index this dependency,
see `quarkus.index-dependency.*`). For each request it:

- resolves the caller identity (it works with both proactive and lazy
  authentication, without blocking the event loop);
- copies every claim listed in `selfcare.logging.mdc.claims` (comma separated,
  default `uid`) into the MDC, using the claim name as key;
- logs a `WARN` when a configured claim is missing in the JWT and a `DEBUG`
  when the request carries no JWT; in both cases nothing is put in the MDC;
- skips Quarkus internal paths (`/q/*`) and never aborts the request;
- removes the claims from the MDC when the response is produced.

To print the claims, reference them in the log format, e.g.:

```
quarkus.log.console.format=%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c{3.}] (%t) trace_id=%X{trace_id} span_id=%X{span_id} uid=%X{uid} - %s%e%n
```

To log additional claims in the future, extend the property:

```
selfcare.logging.mdc.claims=uid,tenant_id
```

