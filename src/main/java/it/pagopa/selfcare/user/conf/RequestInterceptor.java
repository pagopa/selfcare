package it.pagopa.selfcare.user.conf;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;

import java.util.stream.IntStream;

@Slf4j
@Provider
public class RequestInterceptor {

    private static final String DELIMITER = ";";

    @ServerRequestFilter(preMatching = true)
    public void preMatchingFilter(ContainerRequestContext requestContext) {

        var queryParams = requestContext.getUriInfo().getQueryParameters();
        UriBuilder uriBuilder = requestContext.getUriInfo().getRequestUriBuilder().clone();

        queryParams.entrySet().stream().forEach(el -> {
            if (el.getValue().size() == 1  && el.getValue().get(0).contains(DELIMITER)) {
                var values = el.getValue().get(0).split(DELIMITER);
                IntStream.range(0, values.length)
                        .forEach(index -> {
                            if(index == 0)
                                uriBuilder.replaceQueryParam(el.getKey(), values[index]);
                            else
                                uriBuilder.queryParam(el.getKey(), values[index]);
                        });
            }
        });

        requestContext.setRequestUri(uriBuilder.build());

    }
}
