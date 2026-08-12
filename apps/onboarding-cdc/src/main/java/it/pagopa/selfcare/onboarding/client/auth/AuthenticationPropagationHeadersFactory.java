package it.pagopa.selfcare.onboarding.client.auth;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.MultivaluedMap;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

import java.util.List;
import java.util.Objects;

@Slf4j
@ApplicationScoped
public class AuthenticationPropagationHeadersFactory implements ClientHeadersFactory {

    private static final String JWT_BEARER_TOKEN_ENV = "JWT_BEARER_TOKEN";

    /** HTTP header carrying the tenant identifier; mirrors {@code TenantConstants#TENANT_HEADER}. */
    private static final String TENANT_HEADER = "X-Tenant-Id";

    public MultivaluedMap<String, String> update(MultivaluedMap<String, String> incomingHeaders, MultivaluedMap<String, String> clientOutgoingHeaders) {
        log.trace("AuthenticationPropagationHeadersFactory - incomingHeaders: {}", incomingHeaders.keySet());
        log.trace("AuthenticationPropagationHeadersFactory - clientOutgoingHeaders: {}", clientOutgoingHeaders.keySet());

        final String bearerToken = System.getenv(JWT_BEARER_TOKEN_ENV);

        if (Objects.isNull(bearerToken)) {
            log.warn("AuthenticationPropagationHeadersFactory - JWT_BEARER_TOKEN environment variable is not set, skipping Authorization header");
            return clientOutgoingHeaders;
        }

        log.trace("AuthenticationPropagationHeadersFactory - JWT_BEARER_TOKEN is present, length: {}",
                bearerToken.length());

        final long periodCount = bearerToken.chars().filter(c -> c == '.').count();
        log.trace("AuthenticationPropagationHeadersFactory - JWT period count (expected 2): {}", periodCount);

        clientOutgoingHeaders.put("Authorization", List.of("Bearer " + bearerToken));
        log.debug("AuthenticationPropagationHeadersFactory - Authorization header set successfully");

        // registry-proxy enforces tenant consistency on every authenticated call (Step_0 SELC-5):
        // a JWT with no X-Tenant-Id header is rejected with 400. The header is derived from the
        // machine token itself so the two can never disagree; if the provisioned token carries no
        // tenant claim no header is sent, because the receiving service would reject the request
        // for the missing claim regardless and a fabricated header would only mask the real cause.
        MachineTokenTenantResolver.tenantOf(bearerToken)
                .ifPresent(tenant -> clientOutgoingHeaders.put(TENANT_HEADER, List.of(tenant)));
        return clientOutgoingHeaders;
    }
}


