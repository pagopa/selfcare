package it.pagopa.selfcare.party.registry_proxy.connector.rest.service;

import io.dapr.client.DaprClient;
import it.pagopa.selfcare.onboarding.crypto.utils.DataEncryptionUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class StorageAsyncService {

    private final DaprClient daprSelcClient;

    public StorageAsyncService(DaprClient daprSelcClient) {
        this.daprSelcClient = daprSelcClient;
    }

    @Async("storageTaskExecutor")
    public void saveStringToStorage(String document, String keyName) {
        try {
            daprSelcClient.saveState("blobstorage-state", keyName, document).block();
            log.info("Document saved into Azure Storage with key: {}", sanitizeForLog(keyName));
        } catch (Exception e) {
            log.error("Impossible to store document with key {} into Azure Storage", sanitizeForLog(keyName), e);
        }
    }

    /**
     * Sanitize the input string by removing line breaks and carriage returns
     * to prevent log injection.
     */
    private static String sanitizeForLog(String input) {
        if (input == null) return null;
        return input.replaceAll("[\\p{Cntrl}]", "");
    }
}
