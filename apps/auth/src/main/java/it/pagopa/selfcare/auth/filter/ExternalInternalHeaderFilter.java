package it.pagopa.selfcare.auth.filter;

import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ExternalInternalHeaderFilter extends ApimHeaderFilter implements ClientRequestFilter {

    @Inject
    @ConfigProperty(name = "internal.api.key")
    String apiKey;

    @Override
    public void filter(ClientRequestContext requestContext) {
        injectApimKey(requestContext, apiKey);
    }
}
