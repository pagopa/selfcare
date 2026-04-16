package it.pagopa.selfcare.onboarding.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.quarkus.runtime.StartupEvent;
import io.vertx.core.json.jackson.DatabindCodec;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.azurestorage.AzureBlobClientDefault;
import it.pagopa.selfcare.onboarding.crypto.*;
import it.pagopa.selfcare.onboarding.repository.OnboardingRepository;
import it.pagopa.selfcare.product.service.ProductService;
import it.pagopa.selfcare.product.service.ProductServiceCacheable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

@ApplicationScoped
public class OnboardingFunctionConfig {

    private static final Logger log = LoggerFactory.getLogger(OnboardingFunctionConfig.class);
    public static final String SIGNATURE_SOURCE_ARUBA = "aruba";
    public static final String SIGNATURE_SOURCE_NAMIRIAL = "namirial";
    public static final String SIGNATURE_SOURCE_DISABLED = "disabled";

    void onStart(@Observes StartupEvent ev, OnboardingRepository repository) {
        log.info(String.format("Database %s is starting...", repository.mongoDatabase().getName()));
    }

    @Produces
    public ObjectMapper objectMapper(){
        ObjectMapper mapper =  DatabindCodec.mapper();
        //mapper.registerModule(new ParameterNamesModule(JsonCreator.Mode.PROPERTIES)); // mandatory config

        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);// custom config
        mapper.registerModule(new JavaTimeModule());                               // custom config
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);            // custom config
       // mapper.registerModule(new Jdk8Module());                                   // custom config
        return mapper;
    }

    @ApplicationScoped
    public AzureBlobClient azureBobClientContract(AzureStorageConfig azureStorageConfig){
        return new AzureBlobClientDefault(azureStorageConfig.connectionStringContract(), azureStorageConfig.containerContract());
    }

    @ApplicationScoped
    public ProductService productService(AzureStorageConfig azureStorageConfig){
       return new ProductServiceCacheable(azureStorageConfig.connectionStringProduct(), azureStorageConfig.containerProduct(), azureStorageConfig.productFilepath());
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
    public PadesSignService padesSignService(@ConfigProperty(name = "onboarding-functions.pagopa-signature.source") String source){
        return switch (source) {
            case SIGNATURE_SOURCE_ARUBA -> new PadesSignServiceImpl(arubaPkcs7HashSignService());
            case SIGNATURE_SOURCE_NAMIRIAL -> new PadesSignServiceImpl(namirialPkcs7HashSignService());
            case SIGNATURE_SOURCE_DISABLED -> new PadesSignServiceImpl(disabledPkcs7HashSignService());
            default -> new PadesSignServiceImpl(pkcs7HashSignService());
        };
    }
}
