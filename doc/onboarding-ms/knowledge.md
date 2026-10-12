# Tenant propagation to onboarding-functions

## Context

`onboarding-ms` validates `X-Tenant-Id` at ingress but the generated Functions client previously only supplied the function key.

## Cause

The OpenAPI generator owns the client header factory needed for the function key, so the standard application header factory was not registered on this client.

## Fix

Register `TenantHeaderClientRequestFilter` on every generated `onboarding_functions_json` client. It forwards only the tenant resolved by the request context.

## Verification

The configuration test asserts that all generated Functions clients register the filter and retain function-key authentication.

## Prevention

Any new generated client to onboarding-functions must retain this provider registration.
