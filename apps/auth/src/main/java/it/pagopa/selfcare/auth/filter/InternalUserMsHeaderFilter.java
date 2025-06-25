package it.pagopa.selfcare.auth.filter;

import jakarta.inject.Inject;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Provider
public class InternalUserMsHeaderFilter extends ApimHeaderFilter implements ClientRequestFilter {

    @Inject
    @ConfigProperty(name = "internal.user-ms.api.key")
    String apiKey;

    @Override
    public void filter(ClientRequestContext requestContext) {
        injectApimKey(requestContext, apiKey);
    }
}
