package it.pagopa.selfcare.commons.health;

import io.smallrye.health.api.AsyncHealthCheck;
import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Base class for reactive (Mutiny) {@link AsyncHealthCheck}s that verify the <b>actual reachability</b>
 * of a downstream dependency.
 *
 * <p>Subclasses only have to provide:
 * <ul>
 *   <li>{@link #checkName()} — the health check identifier surfaced under {@code /q/health/ready};</li>
 *   <li>{@link #probe()} — a Mutiny {@link Uni} that triggers a lightweight round-trip
 *       (e.g. HEAD blob, mongo ping, GET on a downstream health endpoint).</li>
 * </ul>
 *
 * <p>This base class transparently handles:
 * <ul>
 *   <li>Per-check timeout (default {@link HealthCheckConstants#DEFAULT_TIMEOUT}, override via {@link #timeout()});</li>
 *   <li>Turning the probe outcome into {@code UP}/{@code DOWN};</li>
 *   <li>Wrapping any failure (including timeout) into a stable {@code data.error} field so operators
 *       can distinguish an authentication failure from a network partition without digging through
 *       logs.</li>
 *   <li>Reporting best-effort latency in {@code data.latencyMs}.</li>
 * </ul>
 *
 * <p><b>Threading model.</b> The returned {@link Uni} is executed on the caller thread of
 * SmallRye Health, which is a Vert.x event-loop when invoked from {@code /q/health/ready}.
 * The {@link #probe()} method <b>MUST NOT</b> block. Wrap any blocking client with
 * {@code Uni.createFrom().item(...).runSubscriptionOn(...)} in the subclass if strictly required.
 */
public abstract class AbstractAsyncReadinessCheck implements AsyncHealthCheck {

    /** Health check identifier. Displayed under {@code /q/health/ready}. */
    protected abstract String checkName();

    /**
     * The actual downstream probe. Must be a non-blocking {@link Uni}.
     * The returned item value is ignored — only completion vs. failure matters.
     */
    protected abstract Uni<?> probe();

    /**
     * Per-check timeout. Override to tune. Defaults to
     * {@link HealthCheckConstants#DEFAULT_TIMEOUT}.
     */
    protected Duration timeout() {
        return HealthCheckConstants.DEFAULT_TIMEOUT;
    }

    /**
     * Extra static metadata to attach to every response (UP or DOWN).
     * Useful to surface {@code account}, {@code container}, {@code database} etc.
     * The default is empty.
     */
    protected Map<String, String> data() {
        return Collections.emptyMap();
    }

    @Override
    public final Uni<HealthCheckResponse> call() {
        final long startNanos = System.nanoTime();
        final Duration timeout = timeout();

        return probe()
                .ifNoItem().after(timeout).failWith(() -> new TimeoutException(
                        "Readiness probe '" + checkName() + "' timed out after " + timeout))
                .onItemOrFailure().transform((item, failure) -> {
                    final long latencyMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
                    final HealthCheckResponseBuilder builder = HealthCheckResponse.named(checkName())
                            .withData(HealthCheckConstants.DATA_KEY_LATENCY_MS, String.valueOf(latencyMs));
                    data().forEach(builder::withData);

                    if (Objects.isNull(failure)) {
                        return builder.up().build();
                    }
                    return builder
                            .down()
                            .withData(HealthCheckConstants.DATA_KEY_ERROR,
                                    failure.getClass().getSimpleName() + ": " + safeMessage(failure))
                            .build();
                });
    }

    private static String safeMessage(Throwable t) {
        final String msg = t.getMessage();
        return Objects.isNull(msg) ? "" : msg;
    }
}

