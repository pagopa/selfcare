package it.pagopa.selfcare.commons.health.spring;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AbstractReadinessIndicatorTest {

    @Test
    void returnsUpWithMetadataAndLatencyWhenProbeSucceeds() {
        AbstractReadinessIndicator indicator = new TestReadinessIndicator(
                () -> {
                },
                Duration.ofSeconds(1),
                Map.of("component", "downstream"));

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails())
                .containsEntry("component", "downstream")
                .containsKey("latencyMs")
                .doesNotContainKey("error");
    }

    @Test
    void returnsDownWithStableErrorWhenProbeFails() {
        AbstractReadinessIndicator indicator = new TestReadinessIndicator(
                () -> {
                    throw new IllegalStateException("unavailable");
                },
                Duration.ofSeconds(1),
                Map.of());

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails())
                .containsEntry("error", "IllegalStateException: unavailable")
                .containsKey("latencyMs");
    }

    @Test
    void returnsDownWhenProbeTimesOut() {
        AbstractReadinessIndicator indicator = new TestReadinessIndicator(
                () -> Thread.sleep(Duration.ofSeconds(5).toMillis()),
                Duration.ofMillis(20),
                Map.of());

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails().get("error").toString())
                .startsWith("TimeoutException: Readiness probe 'test-readiness' timed out");
    }

    private static final class TestReadinessIndicator extends AbstractReadinessIndicator {

        private final CheckedProbe probe;
        private final Duration timeout;
        private final Map<String, Object> data;

        private TestReadinessIndicator(
                CheckedProbe probe,
                Duration timeout,
                Map<String, Object> data) {
            this.probe = probe;
            this.timeout = timeout;
            this.data = data;
        }

        @Override
        protected String checkName() {
            return "test-readiness";
        }

        @Override
        protected void probe() throws Exception {
            probe.run();
        }

        @Override
        protected Duration timeout() {
            return timeout;
        }

        @Override
        protected Map<String, Object> data() {
            return data;
        }
    }

    @FunctionalInterface
    private interface CheckedProbe {
        void run() throws Exception;
    }
}
