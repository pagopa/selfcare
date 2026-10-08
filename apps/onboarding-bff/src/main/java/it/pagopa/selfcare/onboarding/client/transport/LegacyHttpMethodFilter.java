package it.pagopa.selfcare.onboarding.client.transport;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import java.net.ProtocolException;
import java.util.Set;

/**
 * Preserves the HttpURLConnection method boundary of the Spring user-registry client.
 * In particular, PATCH failed before sending anything; enabling those writes is a separate behavior change.
 */
public final class LegacyHttpMethodFilter implements ClientRequestFilter {

    private static final Set<String> METHODS = Set.of("GET", "POST", "HEAD", "OPTIONS", "PUT", "DELETE", "TRACE");

    @Override
    public void filter(ClientRequestContext context) throws ProtocolException {
        if (!METHODS.contains(context.getMethod())) {
            throw new ProtocolException("Invalid HTTP method: " + context.getMethod());
        }
    }
}
