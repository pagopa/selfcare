package it.pagopa.selfcare.document.config;

import io.quarkus.runtime.StartupEvent;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.azurestorage.AzureBlobClientDefault;
import it.pagopa.selfcare.document.model.entity.Document;
import it.pagopa.selfcare.onboarding.crypto.*;
import it.pagopa.selfcare.product.service.ProductService;
import it.pagopa.selfcare.product.service.ProductServiceCacheable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.InputStream;

@ApplicationScoped
@Slf4j
@Data
public class DocumentMsConfig {

    public static final String SIGNATURE_SOURCE_ARUBA = "aruba";
    public static final String SIGNATURE_SOURCE_NAMIRIAL = "namirial";
    public static final String SIGNATURE_SOURCE_DISABLED = "disabled";
    public static final String PDF_FORMAT_FILENAME = "%s_accordo_adesione.pdf";


    @ConfigProperty(name = "document-ms.blob-storage.container-product")
    String containerProduct;

    @ConfigProperty(name = "document-ms.blob-storage.filepath-product")
    String filepathProduct;

    @ConfigProperty(name = "document-ms.blob-storage.connection-string-product")
    String connectionStringProduct;

    @ConfigProperty(name = "document-ms.blob-storage.path-contracts")
    String contractPath;

    @ConfigProperty(name = "document-ms.blob-storage.path-aggregates")
    String aggregatesPath;

    @ConfigProperty(name = "document-ms.blob-storage.path-deleted")
    String deletePath;

    void onStart(@Observes StartupEvent ev) {
        log.info("Database {} is starting...", Document.mongoDatabase().getName());
    }

    @ApplicationScoped
    public ProductService productService(){
        return new ProductServiceCacheable(connectionStringProduct, containerProduct, filepathProduct);
    }

    @ApplicationScoped
    public AzureBlobClient azureBobClientContract(@ConfigProperty(name = "document-ms.blob-storage.connection-string-contracts")
                                                      String connectionStringContracts,
                                                  @ConfigProperty(name = "document-ms.blob-storage.container-contracts")
                                                      String containerContracts){
        return new AzureBlobClientDefault(connectionStringContracts, containerContracts);
    }

    public Pkcs7HashSignService arubaPkcs7HashSignService(){
        log.info("Signature will be performed using ArubaPkcs7HashSignServiceImpl");
        return new ArubaPkcs7HashSignServiceImpl(new ArubaSignServiceImpl());
    }

    public Pkcs7HashSignService namirialPkcs7HashSignService(){
        log.info("Signature will be performed using NamirialPkcs7HashSignServiceImpl");
        return new NamirialPkcs7HashSignServiceImpl(new NamiralSignServiceImpl());
    }


    public Pkcs7HashSignService disabledPkcs7HashSignService(){
        log.info("Signature will be performed using Pkcs7HashSignService");
        return new Pkcs7HashSignService(){
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

    public Pkcs7HashSignService pkcs7HashSignService(){
        return new Pkcs7HashSignServiceImpl();
    }
    @ApplicationScoped
    public PadesSignService padesSignService(@ConfigProperty(name = "document-ms.pagopa-signature.source") String source){
        return switch (source) {
            case SIGNATURE_SOURCE_ARUBA -> new PadesSignServiceImpl(arubaPkcs7HashSignService());
            case SIGNATURE_SOURCE_NAMIRIAL -> new PadesSignServiceImpl(namirialPkcs7HashSignService());
            case SIGNATURE_SOURCE_DISABLED -> new PadesSignServiceImpl(disabledPkcs7HashSignService());
            default -> new PadesSignServiceImpl(pkcs7HashSignService());
        };
    }
}
