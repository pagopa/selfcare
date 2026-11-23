package it.pagopa.selfcare.user.client.auth;

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

    @Inject TenantContext tenantContext;
    @Inject TenantRegistry tenantRegistry;

    static final String AUTHORIZATION_HEADER = "Authorization";
    static final String TENANT_HEADER = "X-Tenant-Id";

    @Override
    public MultivaluedMap<String, String> update(MultivaluedMap<String, String> incomingHeaders, MultivaluedMap<String, String> clientOutgoingHeaders) {
        copyHeader(incomingHeaders, clientOutgoingHeaders, AUTHORIZATION_HEADER);
        List<String> incomingTenants = tenantHeaders(incomingHeaders);
        String tenant = tenantRegistry.normalizeTenantId(tenantContext.isInitialized()
                ? tenantContext.requiredTenantId() : incomingTenants.stream().findFirst().orElse(null));
        tenantRegistry.resolve(tenant);
        validateHeader(incomingTenants, tenant);
        validateHeader(tenantHeaders(clientOutgoingHeaders), tenant);
        clientOutgoingHeaders.keySet().removeIf(TENANT_HEADER::equalsIgnoreCase);
        clientOutgoingHeaders.putSingle(TENANT_HEADER, tenant);
        return clientOutgoingHeaders;
    }

    private List<String> tenantHeaders(MultivaluedMap<String, String> headers) {
        return headers.entrySet().stream()
                .filter(entry -> TENANT_HEADER.equalsIgnoreCase(entry.getKey()) && entry.getValue() != null)
                .flatMap(entry -> entry.getValue().stream())
                .filter(java.util.Objects::nonNull).toList();
    }

    private void validateHeader(List<String> values, String tenant) {
        if (values != null && values.stream().anyMatch(value ->
                value != null && !value.isBlank() && !tenant.equals(tenantRegistry.normalizeTenantId(value)))) {
            throw new BadRequestException("Conflicting tenant context");
        }
    }

    private static void copyHeader(
            MultivaluedMap<String, String> incomingHeaders,
            MultivaluedMap<String, String> clientOutgoingHeaders,
            String headerName) {
        if (incomingHeaders.containsKey(headerName)) {
            List<String> headerValue = incomingHeaders.get(headerName);
            if (headerValue != null) {
                clientOutgoingHeaders.put(headerName, headerValue);
            }
        }
    }
}