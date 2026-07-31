package it.pagopa.selfcare.onboarding.web.security.tenant;

/**
 * Minimal RFC 7807-style ("application/problem+json") payload for tenant validation failures.
 * Kept self-contained so it does not depend on any app-specific Problem model.
 */
public record TenantProblem(int status, String title, String detail) {
}
