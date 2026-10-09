# Tenant propagation to onboarding-functions

## Context

`onboarding-cdc` sends notification events to onboarding-functions asynchronously.

## Cause

The generated Functions client supplied its function key but did not invoke the tenant propagation factory.

## Fix

Register `TenantHeaderClientRequestFilter` on the generated Functions client. The CDC forwards its current tenant or the configured tenant of its stack; it never forwards the bearer token.

## Verification

The configuration test validates the provider registration, and the filter test verifies default and scoped tenants.

## Prevention

Keep the provider registration on every generated onboarding-functions client.
