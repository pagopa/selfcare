# Onboarding CDC

The service watches onboarding changes and invokes the notification HTTP function
for events that must be propagated to downstream consumers.

## Onboarding Functions requester identity

Notification calls do not send `x-selfcare-uid`: onboarding-functions derives the
caller identity (`userId` in Application Insights) only from the `uid` claim of the
verified `JWT_BEARER_TOKEN`, so the technical token identifies onboarding-cdc.

The client sends the Azure Function key, `X-Tenant-Id`, and the configured
`JWT_BEARER_TOKEN`. The client does not inspect the token: onboarding-functions
(`HttpJwtValidator`) is the only place that verifies signature, `exp`, issuer,
`uid` and `tenant_id` against `X-Tenant-Id`.

`JWT_BEARER_TOKEN` is an expiring Key Vault secret: rotate it before `exp` and
keep its signing key aligned with the target stack's public key. Each deployment
must remain tenant-specific; a token minted for AR cannot be reused by PNPG (or
vice versa).

## Tests

Run the module tests from the repository root with:

```shell
mvn -pl apps/onboarding-cdc test
```
