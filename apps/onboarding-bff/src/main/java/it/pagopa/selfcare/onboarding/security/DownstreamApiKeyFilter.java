package it.pagopa.selfcare.onboarding.security;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Provider
@ApplicationScoped
@Priority(Priorities.AUTHENTICATION)
public class DownstreamApiKeyFilter implements ClientRequestFilter {

    @ConfigProperty(name = "rest-client.user-registry.api-key")
    String apiKey;

    @Override
    public void filter(ClientRequestContext request) {
        // The Spring interceptor is global, including product, document, IAM and Functions clients.
        request.getHeaders().putSingle("x-api-key", apiKey);
    }
}
