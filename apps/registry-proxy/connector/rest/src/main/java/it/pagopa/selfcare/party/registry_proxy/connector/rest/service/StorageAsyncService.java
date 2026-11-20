package it.pagopa.selfcare.party.registry_proxy.connector.rest.service;

import com.azure.core.util.BinaryData;
import it.pagopa.selfcare.commons.tenant.storage.TenantBlobClientProvider;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class StorageAsyncService {

    static final String VISURE_STORAGE = "visure";
    private final TenantBlobClientProvider blobClients;

    public StorageAsyncService(TenantBlobClientProvider blobClients) {
        this.blobClients = blobClients;
    }

    @Async("storageTaskExecutor")
    public CompletableFuture<Void> saveStringToStorage(String tenantId, String document, String keyName) {
        blobClients.clientFor(tenantId, VISURE_STORAGE)
                .blob(keyName).upload(BinaryData.fromString(document), true);
        log.info("Document archived for tenant {}", tenantId);
        return CompletableFuture.completedFuture(null);
    }
}
