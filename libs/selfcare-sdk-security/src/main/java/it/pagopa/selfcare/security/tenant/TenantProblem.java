package it.pagopa.selfcare.security.tenant;

/**
 * Minimal RFC 7807-style ("application/problem+json") payload for tenant validation failures.
 * Kept self-contained in this shared library so it does not depend on any app-specific
 * {@code Problem} model, avoiding a reverse dependency from the security library to each app.
 */
public record TenantProblem(int status, String title, String detail) {
}
