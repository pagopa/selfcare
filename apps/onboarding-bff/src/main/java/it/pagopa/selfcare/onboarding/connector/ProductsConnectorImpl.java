package it.pagopa.selfcare.onboarding.connector;

import it.pagopa.selfcare.commons.base.logging.LogUtils;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Objects;

@Slf4j
@ApplicationScoped
public class ProductsConnectorImpl {
    private final ProductService productService;

    public ProductsConnectorImpl(ProductService productService) {
        this.productService = productService;
    }
    public Product getProduct(String id, InstitutionType institutionType) {
        if (id.matches("\\w*")) {
            log.debug(LogUtils.CONFIDENTIAL_MARKER, "getProduct id = {}", id);
        }
        requireHasText(id, "A productId is required");
        Product product = productService.getProduct(id);

        log.debug(LogUtils.CONFIDENTIAL_MARKER, "getProduct result = {}", product);
        return product;
    }
    public Product getProductValid(String id) {
        if (id.matches("\\w*")) {
            log.debug(LogUtils.CONFIDENTIAL_MARKER, "getProductValid id = {}", id);
        }
        requireHasText(id, "A productId is required");
        Product result = productService.getProductIsValid(id);
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "getProductValid result = {}", result);
        return result;
    }
    public List<Product> getProducts(boolean rootOnly) {
        List<Product> result = productService.getProducts(rootOnly, true);
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "getProducts result = {}", result);
        return result;
    }

    private static void requireHasText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }

}
