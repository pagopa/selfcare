package it.pagopa.selfcare.commons.health;

import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AbstractMongoReadinessCheckTest {

    private static final class UpMongoCheck extends AbstractMongoReadinessCheck {
        private final String host;

        UpMongoCheck(String host) {
            this.host = host;
        }

        @Override protected String checkName() { return "mongodb-test"; }
        @Override protected String databaseName() { return "selcTest"; }
        @Override protected String host() { return host; }
        @Override protected Uni<?> probe() { return Uni.createFrom().item("ok"); }
    }

    // ------------------------------------------------------------
    // host() hook wiring
    // ------------------------------------------------------------

    @Test
    void data_containsHost_whenHostIsProvided() {
        UpMongoCheck check = new UpMongoCheck("mongo-primary.uat:27017,mongo-secondary.uat:27017");

        HealthCheckResponse response = check.call().await().atMost(Duration.ofSeconds(2));

        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.UP);
        Map<String, Object> data = response.getData().orElseThrow();
        assertThat(data)
                .containsEntry("component", "mongodb")
                .containsEntry("database", "selcTest")
                .containsEntry("host", "mongo-primary.uat:27017,mongo-secondary.uat:27017");
    }

    @Test
    void data_omitsHostKey_whenHostIsBlank_backwardCompatibleWith0_1_0() {
        UpMongoCheck check = new UpMongoCheck("");

        HealthCheckResponse response = check.call().await().atMost(Duration.ofSeconds(2));

        Map<String, Object> data = response.getData().orElseThrow();
        assertThat(data)
                .containsEntry("component", "mongodb")
                .containsEntry("database", "selcTest")
                .doesNotContainKey("host");
    }

    @Test
    void data_omitsHostKey_whenHostIsNull() {
        UpMongoCheck check = new UpMongoCheck(null);

        HealthCheckResponse response = check.call().await().atMost(Duration.ofSeconds(2));

        assertThat(response.getData().orElseThrow()).doesNotContainKey("host");
    }

    // ------------------------------------------------------------
    // hostFromConnectionString utility
    // ------------------------------------------------------------

    @Test
    void hostFromConnectionString_singleHostWithoutCredentials() {
        assertThat(AbstractMongoReadinessCheck.hostFromConnectionString(
                "mongodb://mongo.local:27017/selcTest"))
                .isEqualTo("mongo.local:27017");
    }

    @Test
    void hostFromConnectionString_stripsCredentials() {
        assertThat(AbstractMongoReadinessCheck.hostFromConnectionString(
                "mongodb://user:pwd@mongo.local:27017/db"))
                .isEqualTo("mongo.local:27017");
    }

    @Test
    void hostFromConnectionString_replicaSet_joinsHostsWithComma() {
        assertThat(AbstractMongoReadinessCheck.hostFromConnectionString(
                "mongodb://user:pwd@primary:27017,secondary:27017/db?replicaSet=rs0"))
                .isEqualTo("primary:27017,secondary:27017");
    }

    @Test
    void hostFromConnectionString_returnsPlaceholder_whenNull() {
        assertThat(AbstractMongoReadinessCheck.hostFromConnectionString(null))
                .isEqualTo(AbstractMongoReadinessCheck.HOST_NOT_AVAILABLE);
    }

    @Test
    void hostFromConnectionString_returnsPlaceholder_whenBlank() {
        assertThat(AbstractMongoReadinessCheck.hostFromConnectionString("   "))
                .isEqualTo(AbstractMongoReadinessCheck.HOST_NOT_AVAILABLE);
    }

    @Test
    void hostFromConnectionString_returnsPlaceholder_whenSchemeIsUnknown() {
        assertThat(AbstractMongoReadinessCheck.hostFromConnectionString("postgres://mongo.local:27017"))
                .isEqualTo(AbstractMongoReadinessCheck.HOST_NOT_AVAILABLE);
    }

    @Test
    void hostFromConnectionString_returnsPlaceholder_whenNotAUri() {
        assertThat(AbstractMongoReadinessCheck.hostFromConnectionString("not-a-connection-string"))
                .isEqualTo(AbstractMongoReadinessCheck.HOST_NOT_AVAILABLE);
    }
}
