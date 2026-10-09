package it.pagopa.selfcare.onboarding.config;

import jakarta.annotation.Priority;
import jakarta.ws.rs.ConstrainedTo;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.NotSupportedException;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.ReaderInterceptor;
import jakarta.ws.rs.ext.ReaderInterceptorContext;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;

@Provider
@ConstrainedTo(RuntimeType.SERVER)
@Priority(Priorities.ENTITY_CODER)
public class RequestBodyMediaTypeFilter implements ContainerRequestFilter, ReaderInterceptor {

    static final String BODY_WITHOUT_CONTENT_TYPE = RequestBodyMediaTypeFilter.class.getName() + ".bodyWithoutContentType";

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
        if (multipart) {
            throw new NotSupportedException();
        }
        if (jsonBody) {
            request.setProperty(BODY_WITHOUT_CONTENT_TYPE, true);
        }
    }

    @Override
    public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException {
        // REST has buffered the body or moved its stream to a worker before invoking readers.
        if (Boolean.TRUE.equals(context.getProperty(BODY_WITHOUT_CONTENT_TYPE))
                && context.getInputStream().read() != -1) {
            throw new NotSupportedException();
        }
        return context.proceed();
    }
}
