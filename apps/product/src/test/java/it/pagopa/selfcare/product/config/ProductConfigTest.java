package it.pagopa.selfcare.product.config;

import com.microsoft.applicationinsights.TelemetryClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ProductConfigTest {

    private final ProductConfig config = new ProductConfig();

    @Test
    void telemetryClient_returnsClient() {
        // given
        String connectionString = "InstrumentationKey=00000000-0000-0000-0000-000000000000";

        // when
        TelemetryClient client = config.telemetryClient(connectionString);

        // then
        assertNotNull(client);
    }
}
