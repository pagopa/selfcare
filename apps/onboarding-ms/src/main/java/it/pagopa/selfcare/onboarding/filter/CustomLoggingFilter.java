package it.pagopa.selfcare.onboarding.filter;

import jakarta.annotation.Priority;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveContainerRequestContext;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveContainerRequestFilter;

import java.io.IOException;

@Provider
@Priority(Priorities.USER)
public class CustomLoggingFilter implements ResteasyReactiveContainerRequestFilter {

    private static final Logger LOG = Logger.getLogger(CustomLoggingFilter.class);

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        ResteasyReactiveContainerRequestFilter.super.filter(requestContext);
    }

    @Override
    public void filter(ResteasyReactiveContainerRequestContext requestContext) {
        String endpoint = requestContext.getUriInfo().getPath();
        String method = requestContext.getMethod();
        LOG.infof(
                "Request: tenant: %s, method: %s, endpoint: %s",
                TenantLogUtils.fromInboundRequest(requestContext),
                method,
                endpoint);

    }
}
