package it.pagopa.selfcare.onboarding.steps;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.quarkus.test.junit.TestProfile;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.entity.ProductRole;
import it.pagopa.selfcare.product.entity.ProductRoleInfo;
import it.pagopa.selfcare.product.service.ProductService;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import lombok.extern.slf4j.Slf4j;

@Alternative
@Priority(1)
@ApplicationScoped
@TestProfile(IntegrationProfile.class)
@Slf4j
public class IntegrationProductService implements ProductService {

    private boolean initialized = false;
    private List<Product> products;
    private final CountDownLatch initLatch = new CountDownLatch(1);
    private static final int DEFAULT_EXPIRATION_DATE = 30;

    public void initializeBlocking() {
        if (!initialized) {
            getProducts();
            initialized = true;
            initLatch.countDown();
        }
    }

    private void getProducts() {
        try {
            ClassLoader classLoader = getClass().getClassLoader();
            URL resourceDirectory = classLoader.getResource("integration-data/products.json");

            File jsonFile = new File(resourceDirectory.toURI());
            String content = Files.readString(jsonFile.toPath());
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());

            mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
            products = mapper.readValue(content, new TypeReference<>() {});

        } catch (IOException | URISyntaxException e) {
            System.err.println("Errore nel caricamento dei template JSON: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public List<Product> getProducts(boolean rootOnly, boolean valid) {
        if (!initialized) {
            try {
                initializeBlocking();
            } catch (Exception e) {
                getProducts();
                initialized = true;
                initLatch.countDown();
            }
        }
        return products;
    }

    @Override
    public void validateRoleMappings(Map<PartyRole, ? extends ProductRoleInfo> roleMappings) {

    }

    @Override
    public Product getProduct(String productId) {
        return null;
    }

    @Override
    public Product getProductRaw(String productId) {
        return null;
    }

    @Override
    public Product getProductIsValid(String productId) {
        if (!initialized) {
            try {
                initializeBlocking();
            } catch (Exception e) {
                getProducts();
                initialized = true;
                initLatch.countDown();
            }
        }
    return products.stream()
        .filter(product -> product.getId().equals(productId))
        .findAny()
        .orElse(null);
    }

    @Override
    public ProductRole validateProductRole(String productId, String productRole, PartyRole role) {
        return null;
    }

    @Override
    public boolean verifyAllowedByInstitutionTaxCode(String productId, String taxCode) {
        return false;
    }

    @Override
    public Integer getProductExpirationDate(String productId) {
        return DEFAULT_EXPIRATION_DATE;
    }
}