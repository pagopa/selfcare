package it.pagopa.selfcare.onboarding.filter;


import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientRequestContext;
import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientResponseFilter;

@Provider
public class CustomClientResponseLoggingFilter implements ResteasyReactiveClientResponseFilter {

    private static final Logger LOG = Logger.getLogger(CustomClientResponseLoggingFilter.class);

    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) {
        ResteasyReactiveClientResponseFilter.super.filter(requestContext, responseContext);
    }

    @Override
    public void filter(ResteasyReactiveClientRequestContext requestContext, ClientResponseContext responseContext) {
        String endpoint = requestContext.getUri().getPath();
        String query = requestContext.getUri().getQuery();
        String method = requestContext.getMethod();
        int status = responseContext.getStatus();
        LOG.infof("Response: method: %s, endpoint: %s, query: %s, status [%d]", method, endpoint, query, status);

    }
}
