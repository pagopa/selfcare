package it.pagopa.selfcare.party.registry_proxy.connector.rest.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.dapr.client.DaprClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import reactor.core.publisher.Mono;

@SpringJUnitConfig
@EnableAsync
@TestConfiguration
class StorageAsyncServiceTest {

    @Mock
    private DaprClient daprSelcClient;

    @InjectMocks
    private StorageAsyncService storageAsyncService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        storageAsyncService = new StorageAsyncService(daprSelcClient);
    }

    @Test
    void testSaveStringToStorageSuccess() {
        final String document = "<xml>test</xml>";
        final String keyName = "visura_ABC_2025.xml";

        when(daprSelcClient.saveState(anyString(), anyString(), any()))
                .thenReturn(Mono.empty());
        storageAsyncService.saveStringToStorage(document, keyName);
        verify(daprSelcClient, times(1))
                .saveState("blobstorage-state", keyName, document);
    }

    @Test
    void testSaveStringToStorageError() {
        final String document = "<xml>broken</xml>";
        final String keyName = "visura_ERR_2025.xml";
        when(daprSelcClient.saveState(any(), any(), any())).thenThrow(new IllegalArgumentException());
        assertDoesNotThrow(() -> storageAsyncService.saveStringToStorage(document, keyName));
        verify(daprSelcClient).saveState("blobstorage-state", keyName, document);
    }
}
