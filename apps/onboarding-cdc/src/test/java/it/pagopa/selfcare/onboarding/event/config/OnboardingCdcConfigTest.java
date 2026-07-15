package it.pagopa.selfcare.onboarding.event.config;

import com.azure.data.tables.TableClient;
import com.microsoft.applicationinsights.TelemetryClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class OnboardingCdcConfigTest {

    private final OnboardingCdcConfig config = new OnboardingCdcConfig();

    @Test
    @DisplayName("telemetryClient returns a non-null TelemetryClient for a valid connection string")
    void telemetryClient_returnsClient() {
        // given
        String connectionString = "InstrumentationKey=00000000-0000-0000-0000-000000000000";

        // when
        TelemetryClient client = config.telemetryClient(connectionString);

        // then
        assertNotNull(client);
    }

    @Test
    @DisplayName("tableClient uses connection string when provided")
    void tableClient_withConnectionString() {
        // given
        String connectionString = "DefaultEndpointsProtocol=http;AccountName=fakeaccount;" +
                "AccountKey=ZmFrZWtleWZha2VrZXlmYWtla2V5ZmFrZWtleWZha2VrZXlmYWtla2V5ZmFrZWtleWZha2U=;" +
                "TableEndpoint=http://127.0.0.1:10002/fakeaccount;";

        // when
        TableClient client = config.tableClient(Optional.of(connectionString), "CdCStartAt", Optional.empty(), Optional.empty());

        // then
        assertNotNull(client);
    }

    @Test
    @DisplayName("tableClient uses managed identity when connection string is absent")
    void tableClient_withManagedIdentity() {
        // given / when
        TableClient client = config.tableClient(Optional.empty(), "CdCStartAt", Optional.of("fakestorageaccount"), Optional.of("00000000-0000-0000-0000-000000000000"));

        // then
        assertNotNull(client);
    }

    @Test
    @DisplayName("tableClient uses managed identity when connection string is blank")
    void tableClient_withBlankConnectionString() {
        // given / when
        TableClient client = config.tableClient(Optional.of("  "), "CdCStartAt", Optional.of("fakestorageaccount"), Optional.of("00000000-0000-0000-0000-000000000000"));

        // then
        assertNotNull(client);
    }
}
