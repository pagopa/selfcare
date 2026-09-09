package it.pagopa.selfcare.onboarding.client.auth;

import it.pagopa.selfcare.onboarding.context.TenantContext;
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

    public MultivaluedMap<String, String> update(MultivaluedMap<String, String> incomingHeaders, MultivaluedMap<String, String> clientOutgoingHeaders) {
        log.trace("AuthenticationPropagationHeadersFactory - incomingHeaders: {}", incomingHeaders.keySet());
        log.trace("AuthenticationPropagationHeadersFactory - clientOutgoingHeaders: {}", clientOutgoingHeaders.keySet());

        propagateTenant(incomingHeaders, clientOutgoingHeaders);

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
        return clientOutgoingHeaders;
    }

    private void propagateTenant(MultivaluedMap<String, String> incomingHeaders, MultivaluedMap<String, String> clientOutgoingHeaders) {
        String tenant = incomingHeaders.getFirst(TenantContext.TENANT_HEADER);
        if (tenant == null || tenant.isBlank()) {
            tenant = TenantContext.currentTenant();
        }
        if (tenant != null && !tenant.isBlank()) {
            clientOutgoingHeaders.put(TenantContext.TENANT_HEADER, List.of(TenantContext.resolve(tenant)));
            log.info("Propagating tenant={}", tenant);
        } else {
            // FIXME: This is a temporary solution to avoid the propagation of an empty tenant header. On
            // the Multitenant PHASE2 should be removed
            clientOutgoingHeaders.put(TenantContext.TENANT_HEADER, List.of(TenantContext.resolve("")));
            log.warn("Tenant header is missing in the incoming request, falling back to default tenant");
        }
    }
}


