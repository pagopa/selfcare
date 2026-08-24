package it.pagopa.selfcare.onboarding.config;

import com.microsoft.applicationinsights.TelemetryClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class TelemetryClientProducerTest {

    private final TelemetryClientProducer producer = new TelemetryClientProducer();

    @Test
    void telemetryClient_returnsClient() {
        // given
        String connectionString = "InstrumentationKey=00000000-0000-0000-0000-000000000000";

        // when
        TelemetryClient client = producer.telemetryClient(connectionString);

        // then
        assertNotNull(client);
    }
}
