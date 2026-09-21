package it.pagopa.selfcare.commons.health.spring;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Base Spring Boot Actuator indicator for downstream readiness probes.
 *
 * <p>Implementations provide a lightweight blocking probe. The probe runs on a worker executor,
 * is bounded by {@link #timeout()}, and produces stable latency and error details. Spring WebFlux
 * also supports regular {@link HealthIndicator} beans by adapting them to its reactive health
 * endpoint.
 *
 * <p>Applications must include the indicator bean name in the Actuator readiness group, for
 * example:
 * {@code management.endpoint.health.group.readiness.include=readinessState,myMongoReadiness}.
 */
public abstract class AbstractReadinessIndicator implements HealthIndicator {

    protected abstract String checkName();

    protected abstract void probe() throws Exception;

    protected Duration timeout() {
        return HealthIndicatorConstants.DEFAULT_TIMEOUT;
    }

    protected Map<String, Object> data() {
        return Collections.emptyMap();
    }

    protected Executor executor() {
        return ForkJoinPool.commonPool();
    }

    @Override
    public final Health health() {
        final long startNanos = System.nanoTime();
        final CompletableFuture<Void> probeFuture = CompletableFuture.runAsync(() -> {
            try {
                probe();
            } catch (Exception exception) {
                throw new ProbeException(exception);
            }
        }, executor());

        try {
            probeFuture.get(timeout().toMillis(), TimeUnit.MILLISECONDS);
            return response(Health.up(), startNanos).build();
        } catch (TimeoutException exception) {
            probeFuture.cancel(true);
            TimeoutException timeoutException = new TimeoutException(
                    "Readiness probe '" + checkName() + "' timed out after " + timeout());
            return response(Health.down(), startNanos)
                    .withDetail(HealthIndicatorConstants.DETAIL_ERROR, errorMessage(timeoutException))
                    .build();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            probeFuture.cancel(true);
            return response(Health.down(), startNanos)
                    .withDetail(HealthIndicatorConstants.DETAIL_ERROR, errorMessage(exception))
                    .build();
        } catch (ExecutionException exception) {
            Throwable failure = unwrap(exception);
            return response(Health.down(), startNanos)
                    .withDetail(HealthIndicatorConstants.DETAIL_ERROR, errorMessage(failure))
                    .build();
        }
    }

    private Health.Builder response(Health.Builder builder, long startNanos) {
        long latencyMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
        return builder
                .withDetails(data())
                .withDetail(HealthIndicatorConstants.DETAIL_LATENCY_MS, String.valueOf(latencyMs));
    }

    private static Throwable unwrap(ExecutionException exception) {
        Throwable cause = exception.getCause();
        if (cause instanceof ProbeException && cause.getCause() != null) {
            return cause.getCause();
        }
        return Objects.requireNonNullElse(cause, exception);
    }

    private static String errorMessage(Throwable failure) {
        String message = Objects.requireNonNullElse(failure.getMessage(), "");
        return failure.getClass().getSimpleName() + ": " + message;
    }

    private static final class ProbeException extends RuntimeException {
        private ProbeException(Throwable cause) {
            super(cause);
        }
    }
}
