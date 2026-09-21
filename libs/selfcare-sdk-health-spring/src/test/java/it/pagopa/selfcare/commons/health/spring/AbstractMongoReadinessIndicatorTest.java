package it.pagopa.selfcare.commons.health.spring;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;

class AbstractMongoReadinessIndicatorTest {

    @Test
    void exposesMongoMetadata() {
        AbstractMongoReadinessIndicator indicator = new AbstractMongoReadinessIndicator() {
            @Override
            protected String checkName() {
                return "mongodb-test";
            }

            @Override
            protected String databaseName() {
                return "selfcare";
            }

            @Override
            protected String host() {
                return "mongo-primary:27017,mongo-secondary:27017";
            }

            @Override
            protected void probe() {
            }
        };

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails())
                .containsEntry("component", "mongodb")
                .containsEntry("database", "selfcare")
                .containsEntry("host", "mongo-primary:27017,mongo-secondary:27017");
    }

    @Test
    void extractsHostsWithoutCredentials() {
        String host = AbstractMongoReadinessIndicator.hostFromConnectionString(
                "mongodb://user:password@mongo-primary:27017,mongo-secondary:27017/selfcare");

        assertThat(host).isEqualTo("mongo-primary:27017,mongo-secondary:27017");
    }

    @Test
    void usesPlaceholderForInvalidConnectionString() {
        assertThat(AbstractMongoReadinessIndicator.hostFromConnectionString("invalid"))
                .isEqualTo("n/a");
    }
}
