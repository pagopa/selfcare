package it.pagopa.selfcare.onboarding.client.auth;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class TenantHeaderClientRequestFilter implements ClientRequestFilter {

    private static final String TENANT_HEADER = "X-Tenant-Id";

    private final AuthenticationPropagationHeadersFactory headersFactory;

    @Inject
    public TenantHeaderClientRequestFilter(AuthenticationPropagationHeadersFactory headersFactory) {
        this.headersFactory = headersFactory;
    }

    @Override
    public void filter(ClientRequestContext requestContext) {
        MultivaluedMap<String, String> headers =
                headersFactory.update(new MultivaluedHashMap<>(), requestContext.getStringHeaders());
        List<String> tenant = headers.get(TENANT_HEADER);
        if (tenant != null) {
            requestContext.getHeaders().put(TENANT_HEADER, new ArrayList<>(tenant));
        }
    }
}
