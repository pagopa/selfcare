package it.pagopa.selfcare.onboarding.client.auth;

import it.pagopa.selfcare.onboarding.context.TenantContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.MultivaluedMap;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

import java.util.List;
import java.util.Objects;

@Slf4j
@ApplicationScoped
public class AuthenticationPropagationHeadersFactory implements ClientHeadersFactory {

    @ConfigProperty(name = "onboarding-cdc.jwt-bearer-token", defaultValue = "")
    String bearerToken;

    public MultivaluedMap<String, String> update(MultivaluedMap<String, String> incomingHeaders, MultivaluedMap<String, String> clientOutgoingHeaders) {
        log.trace("AuthenticationPropagationHeadersFactory - incomingHeaders: {}", incomingHeaders.keySet());
        log.trace("AuthenticationPropagationHeadersFactory - clientOutgoingHeaders: {}", clientOutgoingHeaders.keySet());

        String tenant = propagateTenant(incomingHeaders, clientOutgoingHeaders);

        if (Objects.isNull(bearerToken) || bearerToken.isBlank()) {
            throw new IllegalStateException("JWT_BEARER_TOKEN is required to call onboarding-functions");
        }

        TechnicalJwtValidator.validate(bearerToken, tenant);

        clientOutgoingHeaders.put("Authorization", List.of("Bearer " + bearerToken));
        log.debug("AuthenticationPropagationHeadersFactory - Authorization header set successfully");
        return clientOutgoingHeaders;
    }

    private String propagateTenant(MultivaluedMap<String, String> incomingHeaders, MultivaluedMap<String, String> clientOutgoingHeaders) {
        String tenant = incomingHeaders.getFirst(TenantContext.TENANT_HEADER);
        if (tenant == null || tenant.isBlank()) {
            tenant = TenantContext.currentTenant();
        }
        if (tenant != null && !tenant.isBlank()) {
            String resolvedTenant = TenantContext.resolve(tenant);
            clientOutgoingHeaders.put(TenantContext.TENANT_HEADER, List.of(resolvedTenant));
            log.info("Propagating tenant={}", resolvedTenant);
            return resolvedTenant;
        } else {
            // FIXME: This is a temporary solution to avoid the propagation of an empty tenant header. On
            // the Multitenant PHASE2 should be removed
            String resolvedTenant = TenantContext.resolve("");
            clientOutgoingHeaders.put(TenantContext.TENANT_HEADER, List.of(resolvedTenant));
            log.warn("Tenant header is missing in the incoming request, falling back to default tenant");
            return resolvedTenant;
        }
    }
}


