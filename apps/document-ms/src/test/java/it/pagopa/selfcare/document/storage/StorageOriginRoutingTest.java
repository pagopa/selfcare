package it.pagopa.selfcare.document.storage;

import it.pagopa.selfcare.document.model.StorageOrigin;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StorageOriginRoutingTest {

    @Test
    void storageKeyFor_shouldMapSystemAndLegacyNullToContracts() {
        assertEquals(StorageKeys.CONTRACTS, TenantBlobClientProvider.storageKeyFor(StorageOrigin.SYSTEM));
        assertEquals(StorageKeys.CONTRACTS, TenantBlobClientProvider.storageKeyFor(null));
    }

    @Test
    void storageKeyFor_shouldMapUserToUserAttachments() {
        assertEquals(StorageKeys.USER_ATTACHMENTS, TenantBlobClientProvider.storageKeyFor(StorageOrigin.USER));
    }
}
