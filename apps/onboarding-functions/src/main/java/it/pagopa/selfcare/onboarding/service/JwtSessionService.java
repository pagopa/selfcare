package it.pagopa.selfcare.onboarding.service;

public interface JwtSessionService {

    /**
     * Mints a session token for a user, carrying the tenant the current activity is acting for.
     *
     * <p>The tenant claim is required by the downstream tenant filter, which rejects a token whose
     * claim does not match the {@code X-Tenant-Id} header. A {@code null} tenant produces a token
     * without the claim, which is the pre-multitenant behaviour.
     */
    String createJwt(String userId, String tenantId);
}
