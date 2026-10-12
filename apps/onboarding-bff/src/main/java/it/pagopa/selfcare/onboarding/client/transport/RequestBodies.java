package it.pagopa.selfcare.onboarding.client.transport;

import jakarta.interceptor.InvocationContext;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Set;

/** Tells whether a REST client invocation sends a request body. */
final class RequestBodies {

    private static final Set<String> FORM_PARAMETERS = Set.of(
            "jakarta.ws.rs.FormParam", "jakarta.ws.rs.BeanParam", "org.jboss.resteasy.reactive.RestForm");
    private static final Set<String> NON_ENTITY_PACKAGES = Set.of("jakarta.ws.rs.", "org.jboss.resteasy.reactive.Rest");

    private RequestBodies() {
    }

    /**
     * A body is sent when an argument is the JAX-RS entity (the parameter without a JAX-RS binding
     * annotation, e.g. a DTO or a multipart form) or a form field, and it is not null.
     */
    static boolean carriesBody(InvocationContext context) {
        Method method = context.getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] arguments = context.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            if (arguments[i] != null && isBody(parameters[i].getAnnotations())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBody(Annotation[] annotations) {
        boolean entity = true;
        for (Annotation annotation : annotations) {
            String name = annotation.annotationType().getName();
            if (FORM_PARAMETERS.contains(name)) {
                return true;
            }
            if (NON_ENTITY_PACKAGES.stream().anyMatch(name::startsWith)) {
                entity = false;
            }
        }
        return entity;
    }
}
