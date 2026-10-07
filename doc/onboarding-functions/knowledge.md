# Tenant header handling

## Context

Azure Functions normalizes HTTP header names to lowercase.

## Cause

An exact lookup of `X-Tenant-Id` ignored the header after host normalization.

## Fix

Resolve the tenant header without regard to header-name case.

## Verification

Unit tests cover lowercase, canonical, and absent header maps.

## Prevention

All Azure Function HTTP triggers must use `TenantContext.tenantHeader` rather than direct map lookup.
