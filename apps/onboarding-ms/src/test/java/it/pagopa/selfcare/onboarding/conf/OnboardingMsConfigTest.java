package it.pagopa.selfcare.onboarding.conf;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import io.quarkus.runtime.StartupEvent;
import it.pagopa.selfcare.onboarding.crypto.ArubaPkcs7HashSignServiceImpl;
import it.pagopa.selfcare.onboarding.crypto.NamirialPkcs7HashSignServiceImpl;
import it.pagopa.selfcare.onboarding.crypto.PadesSignServiceImpl;
import it.pagopa.selfcare.onboarding.crypto.Pkcs7HashSignService;
import it.pagopa.selfcare.product.service.ProductService;
import java.io.ByteArrayInputStream;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OnboardingMsConfigTest {

    private OnboardingMsConfig config;

    @BeforeEach
    void setUp() {
        config = new OnboardingMsConfig();
        config.productAzureService = mock(ProductService.class);
    }

    @Test
    void onStart_shouldInitializeConfiguredProductService() {
        config.onStart(mock(StartupEvent.class));
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
