package it.pagopa.selfcare.onboarding.client.auth;

import it.pagopa.selfcare.tenant.TenantContext;
import it.pagopa.selfcare.tenant.TenantRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.MultivaluedMap;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

import java.util.List;

@ApplicationScoped
public class AuthenticationPropagationHeadersFactory implements ClientHeadersFactory {

    private static final String AUTHORIZATION = "Authorization";
    private static final String TENANT_HEADER = "X-Tenant-Id";

    private final TenantContext tenantContext;
    private final TenantRegistry tenantRegistry;

    @Inject
    public AuthenticationPropagationHeadersFactory(TenantContext tenantContext, TenantRegistry tenantRegistry) {
        this.tenantContext = tenantContext;
        this.tenantRegistry = tenantRegistry;
    }

    @Override
    public MultivaluedMap<String, String> update(MultivaluedMap<String, String> incomingHeaders, MultivaluedMap<String, String> clientOutgoingHeaders) {
        if(incomingHeaders.containsKey(AUTHORIZATION)) {
            List<String> headerValue = incomingHeaders.get(AUTHORIZATION);

            if (headerValue != null) {
                clientOutgoingHeaders.put(AUTHORIZATION, headerValue);
            }

        }

        String tenant = tenantContext.isInitialized()
                ? tenantContext.requiredTenantId()
                : incomingHeaders.getFirst(TENANT_HEADER);
        if (tenant != null && !tenant.isBlank()) {
            String canonicalTenant = tenantRegistry.normalizeTenantId(tenant);
            tenantRegistry.resolve(canonicalTenant);
            validateTenantHeader(incomingHeaders.get(TENANT_HEADER), canonicalTenant);
            validateTenantHeader(clientOutgoingHeaders.get(TENANT_HEADER), canonicalTenant);
            clientOutgoingHeaders.putSingle(TENANT_HEADER, canonicalTenant);
        }

        return clientOutgoingHeaders;
    }

    private void validateTenantHeader(List<String> values, String tenant) {
        if (values == null) {
            return;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()
                    && !tenant.equals(tenantRegistry.normalizeTenantId(value))) {
                throw new BadRequestException("Conflicting tenant context");
            }
        }
    }
}
