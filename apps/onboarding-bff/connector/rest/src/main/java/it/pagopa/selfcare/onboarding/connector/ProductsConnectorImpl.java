package it.pagopa.selfcare.onboarding.connector;

import it.pagopa.selfcare.commons.base.logging.LogUtils;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.connector.api.ProductMsConnector;
import it.pagopa.selfcare.onboarding.connector.api.ProductsConnector;
import it.pagopa.selfcare.onboarding.connector.model.product.Product;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;

@Slf4j
@Service
public class ProductsConnectorImpl implements ProductsConnector {
    private final ProductMsConnector productMsConnector;

    public ProductsConnectorImpl(ProductMsConnector productMsConnector) {
        this.productMsConnector = productMsConnector;
    }

    @Override
    public Product getProduct(String id, InstitutionType institutionType) {
        if (id.matches("\\w*")) {
            log.debug(LogUtils.CONFIDENTIAL_MARKER, "getProduct id = {}", id);
        }
        Assert.hasText(id, "A productId is required");
        Product product = productMsConnector.getProduct(id);

        log.debug(LogUtils.CONFIDENTIAL_MARKER, "getProduct result = {}", product);
        return product;
    }
    @Override
    public Product getProductValid(String id) {
        if (id.matches("\\w*")) {
            log.debug(LogUtils.CONFIDENTIAL_MARKER, "getProductValid id = {}", id);
        }
        Assert.hasText(id, "A productId is required");
        Product result = productMsConnector.getValidProduct(id);
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "getProductValid result = {}", result);
        return result;
    }

    @Override
    public List<Product> getProducts(boolean rootOnly) {
        List<Product> result = productMsConnector.getProducts(rootOnly);
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "getProducts result = {}", result);
        return result;
    }

}
