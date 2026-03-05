package it.pagopa.selfcare.onboarding.filter;


import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientRequestContext;
import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientRequestFilter;

import java.io.IOException;

@Provider
public class CustomClientRequestLoggingFilter implements ResteasyReactiveClientRequestFilter {

    private static final Logger LOG = Logger.getLogger(CustomClientRequestLoggingFilter.class);

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        ResteasyReactiveClientRequestFilter.super.filter(requestContext);
    }

    @Override
    public void filter(ResteasyReactiveClientRequestContext requestContext) {
        String endpoint = requestContext.getUri().getPath();
        String query = requestContext.getUri().getQuery();
        String method = requestContext.getMethod();
        LOG.infof("Request: method: %s, endpoint: %s, query: %s", method, endpoint, query);
    }
}
