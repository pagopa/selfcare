package it.pagopa.selfcare.onboarding.storage;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import org.junit.jupiter.api.Test;

class PrefixingAzureBlobClientTest {

    @Test
    void wrap_returnsDelegateWhenPrefixIsBlank() {
        AzureBlobClient delegate = mock(AzureBlobClient.class);

        assertSame(delegate, PrefixingAzureBlobClient.wrap(delegate, ""));
        assertSame(delegate, PrefixingAzureBlobClient.wrap(delegate, null));
    }

    @Test
    void wrap_prefixesBlobPaths() {
        AzureBlobClient delegate = mock(AzureBlobClient.class);
        AzureBlobClient prefixed = PrefixingAzureBlobClient.wrap(delegate, "onboarding/");

        prefixed.getFileAsText("products.json");
        prefixed.getProperties("products.json");

        verify(delegate).getFileAsText("onboarding/products.json");
        verify(delegate).getProperties("onboarding/products.json");
    }
}
