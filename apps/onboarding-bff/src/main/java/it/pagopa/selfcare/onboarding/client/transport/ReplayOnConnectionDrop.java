package it.pagopa.selfcare.onboarding.client.transport;

import jakarta.interceptor.InterceptorBinding;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a REST client whose requests without a body are sent once more, immediately, when the
 * connection is lost before the downstream starts to answer.
 *
 * <p>The Spring BFF did the same without declaring it: its Feign clients run on {@code
 * HttpURLConnection}, which repeats such a request once on a new connection (see {@link
 * ReplayOnConnectionDropInterceptor}). Requests with a body, read timeouts, refused connections
 * and failures after the answer began are never repeated.
 */
@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface ReplayOnConnectionDrop {
}
