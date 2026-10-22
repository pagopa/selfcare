package it.pagopa.selfcare.onboarding.client.auth;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import it.pagopa.selfcare.onboarding.context.TenantContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Forwards the tenant to clients that cannot register {@link AuthenticationPropagationHeadersFactory}
 * as {@code ClientHeadersFactory}, because the generator already registers its own one when the spec
 * declares security schemes (onboarding-functions authenticates with the function key).
 * Both {@code X-Tenant-Id} and {@code Authorization} are copied. The function key already
 * generated from the OpenAPI security scheme is preserved.
 */
@ApplicationScoped
public class TenantHeaderClientRequestFilter implements ClientRequestFilter {

    private final AuthenticationPropagationHeadersFactory headersFactory;
    private final String functionKey;

    @Inject
    public TenantHeaderClientRequestFilter(
            AuthenticationPropagationHeadersFactory headersFactory,
            @ConfigProperty(name = "onboarding-cdc.onboarding-functions-api-key")
            String functionKey) {
        this.headersFactory = headersFactory;
        this.functionKey = functionKey;
    }

    @Override
    public void filter(ClientRequestContext requestContext) {
        MultivaluedMap<String, String> headers =
                headersFactory.update(new MultivaluedHashMap<>(), requestContext.getStringHeaders());
        List<String> tenant = headers.get(TenantContext.TENANT_HEADER);
        if (tenant != null) {
            requestContext.getHeaders().put(TenantContext.TENANT_HEADER, new ArrayList<>(tenant));
        }
        List<String> authorization = headers.get("Authorization");
        if (authorization != null) {
            requestContext.getHeaders().put("Authorization", new ArrayList<>(authorization));
        }
        requestContext.getHeaders().putSingle("x-functions-key", functionKey);
    }
}
