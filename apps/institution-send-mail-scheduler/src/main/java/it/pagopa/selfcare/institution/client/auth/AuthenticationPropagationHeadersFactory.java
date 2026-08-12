package it.pagopa.selfcare.institution.client.auth;

import jakarta.ws.rs.core.MultivaluedMap;
import java.util.List;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

/**
 * Authenticates the scheduler's outgoing calls with the pre-provisioned machine token.
 *
 * <p>This scheduler has no incoming request and therefore no session tenant to propagate, but
 * {@code user-ms} enforces tenant consistency on every authenticated call (Step_0 SELC-5): a JWT
 * with no {@code X-Tenant-Id} header is rejected with 400. The header is derived from the machine
 * token itself so that the two can never disagree; if the provisioned token carries no tenant
 * claim, no header is sent, because the receiving service would reject the request for a missing
 * claim regardless and a fabricated header would only obscure the real cause.
 */
public class AuthenticationPropagationHeadersFactory implements ClientHeadersFactory {

    /** HTTP header carrying the tenant identifier; mirrors {@code TenantConstants#TENANT_HEADER}. */
    private static final String TENANT_HEADER = "X-Tenant-Id";

    @Override
    public MultivaluedMap<String, String> update(MultivaluedMap<String, String> incomingHeaders, MultivaluedMap<String, String> clientOutgoingHeaders) {
        String bearerToken = System.getenv("JWT_BEARER_TOKEN");
        clientOutgoingHeaders.put("Authorization", List.of("Bearer " + bearerToken));
        MachineTokenTenantResolver.tenantOf(bearerToken)
                .ifPresent(tenant -> clientOutgoingHeaders.put(TENANT_HEADER, List.of(tenant)));
        return clientOutgoingHeaders;
    }
}
