package it.pagopa.selfcare.onboarding.core;

import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.connector.api.ProductsConnector;
import it.pagopa.selfcare.onboarding.connector.exceptions.ResourceNotFoundException;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.entity.ProductStatus;
import it.pagopa.selfcare.product.exception.ProductNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;

@Slf4j
@Service
public class ProductAzureServiceImpl implements ProductAzureService {

    private final ProductsConnector productsConnector;

    @Autowired
    public ProductAzureServiceImpl(ProductsConnector productsConnector) {
        this.productsConnector = productsConnector;
    }

    @Override
    public Product getProduct(String id, InstitutionType institutionType) {
        log.trace("getProduct start");
        String idSanitized = Encode.forJava(id);
        log.debug("getProduct id = {}, institutionType = {}", idSanitized, institutionType);
        Assert.notNull(id, "ProductId is required");
        try {
            Product product = productsConnector.getProduct(id, institutionType);
            log.debug("getProduct result = {}", product);
            log.trace("getProduct end");
            return product;
        } catch (ProductNotFoundException e) {
            throw new ResourceNotFoundException("No product found with id " + id);
        }
    }

    @Override
    public Product getProductValid(String id) {
        log.trace("getProductValid start");
        log.debug("getProductValid id = {}", id);
        Assert.notNull(id, "ProductId is required");
        Product product = productsConnector.getProductValid(id);
        log.debug("getProductValid result = {}", product);
        log.trace("getProductValid end");
        return product;
    }

    @Override
    public List<Product> getProducts(boolean rootOnly) {
        log.trace("getProducts start");
        List<Product> products = productsConnector.getProducts(rootOnly);
        List<Product> activeProducts = products.stream()
                .filter(product -> ProductStatus.ACTIVE.equals(product.getStatus()))
                .toList();
        log.debug("getProducts result = {}", activeProducts);
        log.trace("getProducts end");
        return activeProducts;
    }

}
