package it.pagopa.selfcare.user.conf;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;

import java.util.Arrays;

@Slf4j
@Provider
public class RequestInterceptor {

    @ServerRequestFilter(preMatching = true)
    public void preMatchingFilter(ContainerRequestContext requestContext) {
        var queryParams = requestContext.getUriInfo().getQueryParameters();
        UriBuilder uriBuilder = requestContext.getUriInfo().getRequestUriBuilder();
        queryParams.entrySet().stream().forEach(el -> {
            if (el.getValue().size() == 1  && el.getValue().get(0).contains(",")) {
                var values = Arrays.asList(el.getValue().get(0).split(","));
                values.forEach(value -> uriBuilder.queryParam(el.getKey(), value));
            }
        });
        requestContext.setRequestUri(uriBuilder.build());
    }
}
