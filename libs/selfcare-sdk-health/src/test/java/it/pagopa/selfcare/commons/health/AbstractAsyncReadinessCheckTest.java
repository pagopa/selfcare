package it.pagopa.selfcare.commons.health;

import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

class AbstractAsyncReadinessCheckTest {

    // --- test helpers ------------------------------------------------------------------------

    private static final class TestCheck extends AbstractAsyncReadinessCheck {
        private final String name;
        private final Uni<?> probe;
        private final Duration timeout;
        private final Map<String, String> data;

        TestCheck(String name, Uni<?> probe, Duration timeout, Map<String, String> data) {
            this.name = name;
            this.probe = probe;
            this.timeout = timeout;
            this.data = data;
        }

        @Override protected String checkName()               { return name; }
        @Override protected Uni<?> probe()                   { return probe; }
        @Override protected Duration timeout()               { return timeout; }
        @Override protected Map<String, String> data()       { return data; }
    }

    private static HealthCheckResponse await(AbstractAsyncReadinessCheck check) {
        return check.call().await().atMost(Duration.ofSeconds(5));
    }

    // --- tests -------------------------------------------------------------------------------

    @Test
    void up_whenProbeSucceeds() {
        TestCheck check = new TestCheck(
                "any-check",
                Uni.createFrom().item("ok"),
                Duration.ofSeconds(1),
                Map.of("component", "test"));

        HealthCheckResponse response = await(check);

        assertThat(response.getName()).isEqualTo("any-check");
        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.UP);
        assertThat(response.getData()).isPresent();
        assertThat(response.getData().get())
                .containsEntry("component", "test")
                .containsKey("latencyMs")
                .doesNotContainKey("error");
    }

    @Test
    void down_whenProbeFails_reportsErrorClassAndMessage() {
        TestCheck check = new TestCheck(
                "failing-check",
                Uni.createFrom().failure(new IllegalStateException("boom")),
                Duration.ofSeconds(1),
                Map.of());

        HealthCheckResponse response = await(check);

        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.DOWN);
        assertThat(response.getData()).isPresent();
        assertThat(response.getData().get())
                .containsEntry("error", "IllegalStateException: boom")
                .containsKey("latencyMs");
    }

    @Test
    void down_whenProbeExceedsTimeout() {
        // Probe that never emits => must be short-circuited by the SDK-level timeout.
        Uni<Object> neverEmits = Uni.createFrom().nothing();

        TestCheck check = new TestCheck(
                "slow-check",
                neverEmits,
                Duration.ofMillis(50),
                Map.of());

        HealthCheckResponse response = await(check);

        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.DOWN);
        assertThat(response.getData()).isPresent();
        assertThat(response.getData().get())
                .hasEntrySatisfying("error", err -> assertThat(err.toString())
                        .startsWith(TimeoutException.class.getSimpleName() + ":"));
    }

    @Test
    void down_whenProbeFailsWithNullMessage() {
        TestCheck check = new TestCheck(
                "null-msg-check",
                Uni.createFrom().failure(new RuntimeException()),
                Duration.ofSeconds(1),
                Map.of());

        HealthCheckResponse response = await(check);

        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.DOWN);
        assertThat(response.getData()).isPresent();
        assertThat(response.getData().get())
                .hasEntrySatisfying("error", err -> assertThat(err.toString())
                        .isEqualTo("RuntimeException: "));
    }

    @Test
    void blobBase_populatesComponentAccountContainerAndProbeTarget() {
        AbstractBlobStorageReadinessCheck blob = new AbstractBlobStorageReadinessCheck() {
            @Override protected String checkName()   { return "blob-x"; }
            @Override protected Uni<?> probe()       { return Uni.createFrom().item("ok"); }
            @Override protected String account()     { return "myaccount"; }
            @Override protected String container()   { return "mycontainer"; }
            @Override protected String probeTarget() { return "canary.json"; }
        };

        HealthCheckResponse response = await(blob);

        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.UP);
        assertThat(response.getData()).isPresent();
        assertThat(response.getData().get())
                .containsEntry("component", "blob-storage")
                .containsEntry("account", "myaccount")
                .containsEntry("container", "mycontainer")
                .containsEntry("probeTarget", "canary.json");
    }

    @Test
    void blobBase_omitsProbeTargetKey_whenSubclassDoesNotOverride() {
        AbstractBlobStorageReadinessCheck blob = new AbstractBlobStorageReadinessCheck() {
            @Override protected String checkName()  { return "blob-y"; }
            @Override protected Uni<?> probe()      { return Uni.createFrom().item("ok"); }
            @Override protected String account()    { return "myaccount"; }
            @Override protected String container()  { return "mycontainer"; }
        };

        HealthCheckResponse response = await(blob);

        assertThat(response.getData().get())
                .containsEntry("component", "blob-storage")
                .containsEntry("container", "mycontainer")
                .doesNotContainKey("probeTarget");
    }

    @Test
    void mongoBase_populatesComponentAndDatabase() {
        AbstractMongoReadinessCheck mongo = new AbstractMongoReadinessCheck() {
            @Override protected String checkName()    { return "mongo-x"; }
            @Override protected Uni<?> probe()        { return Uni.createFrom().item("pong"); }
            @Override protected String databaseName() { return "selc-x"; }
        };

        HealthCheckResponse response = await(mongo);

        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.UP);
        assertThat(response.getData()).isPresent();
        assertThat(response.getData().get())
                .containsEntry("component", "mongodb")
                .containsEntry("database", "selc-x");
    }
}

