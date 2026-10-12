package it.pagopa.selfcare.onboarding.security;

import it.pagopa.selfcare.onboarding.util.SecurityIdentityUtils;
import java.util.Set;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * Tenant rules applied by the Spring BFF to the verified token. Only SPID tokens are checked: they
 * carry the tenant in the {@code tenant_id} claim (PNPG when absent) and the {@code X-Tenant-Id}
 * header must name exactly the same tenant. Tokens of other issuers are not checked.
 */
final class TenantPolicy {

    static final String TENANT_HEADER = "X-Tenant-Id";

    private static final String ISSUER_SPID = "SPID";
    private static final String CLAIM_TENANT_ID = "tenant_id";
    private static final String DEFAULT_TENANT = "PNPG";
    private static final Set<String> SUPPORTED_TENANTS = Set.of("AR", "PNPG");

    private TenantPolicy() {
    }

    static boolean isValid(JsonWebToken jwt, String headerTenant) {
        if (!ISSUER_SPID.equals(jwt.getIssuer())) {
            return true;
        }
        Object claim = SecurityIdentityUtils.rawClaim(jwt, CLAIM_TENANT_ID);
        String tokenTenant;
        if (claim == null) {
            tokenTenant = DEFAULT_TENANT;
        } else if (claim instanceof String value && isSupported(value)) {
            tokenTenant = value;
        } else {
            return false;
        }
        return isSupported(headerTenant) && tokenTenant.equals(headerTenant);
    }

    private static boolean isSupported(String tenant) {
        return tenant != null && !tenant.isBlank() && SUPPORTED_TENANTS.contains(tenant);
    }
}
