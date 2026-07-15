package it.pagopa.selfcare.onboarding.event;

import com.azure.data.tables.TableServiceClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class CdcLifecycleTest {

    private CdcLifecycle buildLifecycle(Optional<String> connectionString,
                                        Optional<String> accountName,
                                        Optional<String> clientId) {
        CdcLifecycle lifecycle = new CdcLifecycle();
        lifecycle.storageConnectionString = connectionString;
        lifecycle.storageAccountName = accountName;
        lifecycle.managedIdentityClientId = clientId;
        lifecycle.tableName = "CdCStartAt";
        return lifecycle;
    }

    @Test
    @DisplayName("buildTableServiceClient uses connection string when provided")
    void buildTableServiceClient_withConnectionString() {
        // given
        String connectionString = "DefaultEndpointsProtocol=http;AccountName=fakeaccount;" +
                "AccountKey=ZmFrZWtleWZha2VrZXlmYWtla2V5ZmFrZWtleWZha2VrZXlmYWtla2V5ZmFrZWtleWZha2U=;" +
                "TableEndpoint=http://127.0.0.1:10002/fakeaccount;";
        CdcLifecycle lifecycle = buildLifecycle(Optional.of(connectionString), Optional.empty(), Optional.empty());

        // when
        TableServiceClient client = lifecycle.buildTableServiceClient();

        // then
        assertNotNull(client);
    }

    @Test
    @DisplayName("buildTableServiceClient uses managed identity when connection string is absent")
    void buildTableServiceClient_withManagedIdentity() {
        // given
        CdcLifecycle lifecycle = buildLifecycle(Optional.empty(), Optional.of("fakestorageaccount"), Optional.of("00000000-0000-0000-0000-000000000000"));

        // when
        TableServiceClient client = lifecycle.buildTableServiceClient();

        // then
        assertNotNull(client);
    }

    @Test
    @DisplayName("buildTableServiceClient uses managed identity when connection string is blank")
    void buildTableServiceClient_withBlankConnectionString() {
        // given
        CdcLifecycle lifecycle = buildLifecycle(Optional.of("   "), Optional.of("fakestorageaccount"), Optional.of("00000000-0000-0000-0000-000000000000"));

        // when
        TableServiceClient client = lifecycle.buildTableServiceClient();

        // then
        assertNotNull(client);
    }
}
