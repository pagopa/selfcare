package it.pagopa.selfcare.onboarding.client.auth;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

import it.pagopa.selfcare.onboarding.context.TenantContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Forwards the tenant to clients that cannot register {@link AuthenticationPropagationHeadersFactory}
 * as {@code ClientHeadersFactory}, because the generator already registers its own one when the spec
 * declares security schemes (onboarding-functions authenticates with the function key).
 * Only {@code X-Tenant-Id} is copied, so the bearer token the factory adds is not sent.
 */
@ApplicationScoped
public class TenantHeaderClientRequestFilter implements ClientRequestFilter {

    private final AuthenticationPropagationHeadersFactory headersFactory;

    @Inject
    public TenantHeaderClientRequestFilter(AuthenticationPropagationHeadersFactory headersFactory) {
        this.headersFactory = headersFactory;
    }

    @Override
    public void filter(ClientRequestContext requestContext) {
        MultivaluedMap<String, String> headers =
                headersFactory.update(new MultivaluedHashMap<>(), requestContext.getStringHeaders());
        List<String> tenant = headers.get(TenantContext.TENANT_HEADER);
        if (tenant != null) {
            requestContext.getHeaders().put(TenantContext.TENANT_HEADER, new ArrayList<>(tenant));
        }
    }
}
