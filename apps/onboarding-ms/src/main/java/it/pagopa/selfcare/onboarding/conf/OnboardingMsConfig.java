package it.pagopa.selfcare.onboarding.conf;

import io.quarkus.runtime.StartupEvent;
import it.pagopa.selfcare.onboarding.crypto.*;
import it.pagopa.selfcare.product.service.ProductService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.InputStream;

@ApplicationScoped
@Slf4j
@Data
public class OnboardingMsConfig {

    public static final String SIGNATURE_SOURCE_ARUBA = "aruba";
    public static final String SIGNATURE_SOURCE_NAMIRIAL = "namirial";
    public static final String SIGNATURE_SOURCE_DISABLED = "disabled";

    @Inject
    ProductService productAzureService;

    void onStart(@Observes StartupEvent ev) {
        log.info("Tenant-aware Mongo configuration is starting");
        log.info("ProductService eagerly initialized: {}", productAzureService.getClass().getSimpleName());
    }

    public Pkcs7HashSignService arubaPkcs7HashSignService() {
        log.info("Signature will be performed using ArubaPkcs7HashSignServiceImpl");
        return new ArubaPkcs7HashSignServiceImpl(new ArubaSignServiceImpl());
    }

    public Pkcs7HashSignService namirialPkcs7HashSignService() {
        log.info("Signature will be performed using NamirialPkcs7HashSignServiceImpl");
        return new NamirialPkcs7HashSignServiceImpl(new NamiralSignServiceImpl());
    }


    public Pkcs7HashSignService disabledPkcs7HashSignService() {
        log.info("Signature will be performed using Pkcs7HashSignService");
        return new Pkcs7HashSignService() {
            @Override
            public boolean returnsFullPdf() {
                return false;
            }

            @Override
            public byte[] sign(InputStream inputStream) {
                log.info("Signature source is disabled, skipping signing input file");
                return new byte[0];
            }
        };
    }

    public Pkcs7HashSignService pkcs7HashSignService() {
        return new Pkcs7HashSignServiceImpl();
    }

    @ApplicationScoped
    public PadesSignService padesSignService(@ConfigProperty(name = "onboarding-ms.pagopa-signature.source") String source) {
        return switch (source) {
            case SIGNATURE_SOURCE_ARUBA -> new PadesSignServiceImpl(arubaPkcs7HashSignService());
            case SIGNATURE_SOURCE_NAMIRIAL -> new PadesSignServiceImpl(namirialPkcs7HashSignService());
            case SIGNATURE_SOURCE_DISABLED -> new PadesSignServiceImpl(disabledPkcs7HashSignService());
            default -> new PadesSignServiceImpl(pkcs7HashSignService());
        };
    }
}
