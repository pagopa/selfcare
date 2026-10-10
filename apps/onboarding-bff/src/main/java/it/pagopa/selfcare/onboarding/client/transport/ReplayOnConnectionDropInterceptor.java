package it.pagopa.selfcare.onboarding.client.transport;

import io.smallrye.mutiny.Uni;
import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * Sends a REST client request once more when the connection is lost before the downstream starts
 * to answer, as the Spring BFF did without declaring it.
 *
 * <p>The Spring Feign clients ran on {@code HttpURLConnection}: on an {@code IOException} (not a
 * timeout) while the response head is read it opens a new connection and writes the request again,
 * once, immediately, unless the request streams a body. Feign always streams bodies, so exactly the
 * requests without a body were repeated (GET, HEAD, DELETE, PUT without body), and every logical
 * attempt of a resilience4j retry costs up to two wire calls. This interceptor reproduces that
 * and nothing else: the application-level {@code @Retry} (3 attempts, 5 s apart) stays where it is.
 */
@ReplayOnConnectionDrop
@Interceptor
@Priority(Interceptor.Priority.LIBRARY_AFTER)
public class ReplayOnConnectionDropInterceptor {

    @AroundInvoke
    Object replay(InvocationContext context) throws Exception {
        if (RequestBodies.carriesBody(context)) {
            return context.proceed();
        }
        Object result;
        try {
            result = context.proceed();
        } catch (Exception failure) {
            if (ConnectionDrops.isDroppedBeforeAnswer(failure)) {
                return context.proceed();
            }
            throw failure;
        }
        if (result instanceof Uni<?> uni) {
            return replayOnDrop(uni, context);
        }
        return result;
    }

    // The client Uni is cold: calling the method again builds a request that has not been sent.
    @SuppressWarnings("unchecked")
    private static <T> Uni<T> replayOnDrop(Uni<T> uni, InvocationContext context) {
        return uni.onFailure(ConnectionDrops::isDroppedBeforeAnswer).recoverWithUni(() -> {
            try {
                return (Uni<T>) context.proceed();
            } catch (Exception failure) {
                return Uni.createFrom().failure(failure);
            }
        });
    }
}
