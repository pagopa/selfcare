package it.pagopa.selfcare.onboarding.config;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.NotSupportedException;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.Provider;
import java.lang.reflect.Method;
import java.util.Arrays;

@Provider
@Priority(Priorities.ENTITY_CODER)
public class RequestBodyMediaTypeFilter implements ContainerRequestFilter {

    @Context
    ResourceInfo resourceInfo;

    @Override
    public void filter(ContainerRequestContext request) {
        Class<?> resourceClass = resourceInfo.getResourceClass();
        if (request.getHeaderString("Content-Type") != null
                || resourceClass == null
                || !resourceClass.getPackageName().equals("it.pagopa.selfcare.onboarding.controller")) {
            return;
        }
        Method method = resourceInfo.getResourceMethod();
        Consumes consumes = method.getAnnotation(Consumes.class);
        if (consumes == null) {
            consumes = resourceClass.getAnnotation(Consumes.class);
        }
        if (consumes == null) {
            return;
        }
        boolean multipart = Arrays.asList(consumes.value()).contains(MediaType.MULTIPART_FORM_DATA);
        boolean jsonBody = Arrays.stream(method.getParameterTypes()).anyMatch(type ->
                type.getPackageName().equals("it.pagopa.selfcare.onboarding.model.dto.request"));
        // REST accepts an absent media type; MVC instead uses octet-stream for a present body.
        if (multipart || (jsonBody && (request.hasEntity() || request.getLength() > 0))) {
            throw new NotSupportedException();
        }
    }
}
