package it.pagopa.selfcare.onboarding.service;

import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.entity.ProductStatus;
import it.pagopa.selfcare.product.exception.ProductNotFoundException;
import it.pagopa.selfcare.product.service.ProductService;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;

import java.util.List;
import java.util.Objects;

@Slf4j
@ApplicationScoped
public class ProductAzureServiceImpl implements ProductAzureService {

    private final ProductService productService;

    public ProductAzureServiceImpl(ProductService productService) {
        this.productService = productService;
    }

    @Override
    public Product getProduct(String id, InstitutionType institutionType) {
        log.trace("getProduct start");
        String idSanitized = Encode.forJava(id);
        log.debug("getProduct id = {}, institutionType = {}", idSanitized, institutionType);
        Objects.requireNonNull(id, "ProductId is required");
        try {
            Product product = productService.getProduct(id);
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
        Objects.requireNonNull(id, "ProductId is required");
        Product product = productService.getProductIsValid(id);
        log.debug("getProductValid result = {}", product);
        log.trace("getProductValid end");
        return product;
    }

    @Override
    public List<Product> getProducts(boolean rootOnly) {
        log.trace("getProducts start");
        List<Product> products = productService.getProducts(rootOnly, true);
        List<Product> activeProducts = products.stream()
                .filter(product -> ProductStatus.ACTIVE.equals(product.getStatus()))
                .toList();
        log.debug("getProducts result = {}", activeProducts);
        log.trace("getProducts end");
        return activeProducts;
    }

}
