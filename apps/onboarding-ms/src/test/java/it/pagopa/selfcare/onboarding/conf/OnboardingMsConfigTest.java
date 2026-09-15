package it.pagopa.selfcare.onboarding.conf;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azure.storage.blob.models.BlobProperties;
import io.quarkus.runtime.StartupEvent;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.azurestorage.AzureBlobClientDefault;
import it.pagopa.selfcare.onboarding.crypto.ArubaPkcs7HashSignServiceImpl;
import it.pagopa.selfcare.onboarding.crypto.NamirialPkcs7HashSignServiceImpl;
import it.pagopa.selfcare.onboarding.crypto.PadesSignServiceImpl;
import it.pagopa.selfcare.onboarding.crypto.Pkcs7HashSignService;
import it.pagopa.selfcare.product.service.ProductService;
import it.pagopa.selfcare.product.service.ProductServiceCacheable;
import java.io.ByteArrayInputStream;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OnboardingMsConfigTest {

    private OnboardingMsConfig config;

    @BeforeEach
    void setUp() {
        config = new OnboardingMsConfig();
        config.containerProduct = "products";
        config.filepathProduct = "products.json";
        config.connectionStringProduct = Optional.empty();
        config.accountNameProduct = Optional.of("storage-account");
        config.managedIdentityClientIdProduct = Optional.of("managed-identity");
        config.productAzureService = mock(ProductService.class);
    }

    @Test
    void onStart_shouldInitializeConfiguredProductService() {
        config.onStart(mock(StartupEvent.class));
    }

    @Test
    void productService_shouldCreateCacheableService() {
        AzureBlobClient blobClient = mock(AzureBlobClient.class);
        BlobProperties properties = new BlobProperties(
                OffsetDateTime.now(), OffsetDateTime.now(), null, 0L, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, Map.of(), null);
        when(blobClient.getProperties("products.json")).thenReturn(properties);
        when(blobClient.getFileAsText("products.json"))
                .thenReturn("[{\"id\":\"product-id\",\"title\":\"Product\"}]");

        ProductService result = config.productService(blobClient);

        assertInstanceOf(ProductServiceCacheable.class, result);
    }

    @Test
    void productBlobClient_shouldUseConnectionStringWhenConfigured() {
        config.connectionStringProduct = Optional.of("UseDevelopmentStorage=true");

        assertInstanceOf(AzureBlobClientDefault.class, config.productBlobClient());
    }

    @Test
    void productBlobClient_shouldUseManagedIdentityWhenConnectionStringIsMissing() {
        assertInstanceOf(AzureBlobClientDefault.class, config.productBlobClient());
    }

    @Test
    void productBlobClient_shouldUseManagedIdentityWhenConnectionStringIsBlank() {
        config.connectionStringProduct = Optional.of(" ");

        assertInstanceOf(AzureBlobClientDefault.class, config.productBlobClient());
    }

    @Test
    void arubaPkcs7HashSignService_shouldCreateArubaImplementation() {
        assertInstanceOf(ArubaPkcs7HashSignServiceImpl.class, config.arubaPkcs7HashSignService());
    }

    @Test
    void namirialPkcs7HashSignService_shouldCreateNamirialImplementation() {
        assertInstanceOf(NamirialPkcs7HashSignServiceImpl.class, config.namirialPkcs7HashSignService());
    }

    @Test
    void disabledPkcs7HashSignService_shouldReturnEmptySignature() throws Exception {
        Pkcs7HashSignService service = config.disabledPkcs7HashSignService();

        assertFalse(service.returnsFullPdf());
        assertArrayEquals(new byte[0], service.sign(new ByteArrayInputStream(new byte[]{1})));
    }

    @Test
    void padesSignService_shouldSelectConfiguredSignatureSource() {
        OnboardingMsConfig spy = spy(config);
        Pkcs7HashSignService aruba = mock(Pkcs7HashSignService.class);
        Pkcs7HashSignService namirial = mock(Pkcs7HashSignService.class);
        Pkcs7HashSignService disabled = mock(Pkcs7HashSignService.class);
        Pkcs7HashSignService local = mock(Pkcs7HashSignService.class);
        doReturn(aruba).when(spy).arubaPkcs7HashSignService();
        doReturn(namirial).when(spy).namirialPkcs7HashSignService();
        doReturn(disabled).when(spy).disabledPkcs7HashSignService();
        doReturn(local).when(spy).pkcs7HashSignService();

        List.of(
                spy.padesSignService(OnboardingMsConfig.SIGNATURE_SOURCE_ARUBA),
                spy.padesSignService(OnboardingMsConfig.SIGNATURE_SOURCE_NAMIRIAL),
                spy.padesSignService(OnboardingMsConfig.SIGNATURE_SOURCE_DISABLED),
                spy.padesSignService("local")
        ).forEach(service -> assertInstanceOf(PadesSignServiceImpl.class, service));
        verify(spy).arubaPkcs7HashSignService();
        verify(spy).namirialPkcs7HashSignService();
        verify(spy).disabledPkcs7HashSignService();
        verify(spy).pkcs7HashSignService();
    }
}
