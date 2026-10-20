package it.pagopa.selfcare.mscore.connector.rest;

import it.pagopa.selfcare.commons.base.logging.LogUtils;
import it.pagopa.selfcare.commons.tenant.TenantContext;
import it.pagopa.selfcare.commons.tenant.TenantRegistry;
import it.pagopa.selfcare.mscore.api.ProductConnector;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Slf4j
@Service
public class ProductConnectorImpl implements ProductConnector {

    private final ProductService productService;
    private final TenantContext tenantContext;
    private final TenantRegistry tenantRegistry;

    public ProductConnectorImpl(
            ProductService productService,
            TenantContext tenantContext,
            TenantRegistry tenantRegistry) {
        this.productService = productService;
        this.tenantContext = tenantContext;
        this.tenantRegistry = tenantRegistry;
    }

    @Override
    public Product getProductById(String productId) {
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "getProductById productId = {}", productId);
        Assert.hasText(productId, "A productId is required");
        tenantRegistry.normalizeAndValidate(tenantContext.requiredTenantId());
        Product result = productService.getProduct(productId);
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "getProductById result = {}", result);
        return result;
    }

    @Override
    public Product getProductValidById(String productId) {
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "getProductValidById productId = {}", productId);
        Assert.hasText(productId, "A productId is required");
        tenantRegistry.normalizeAndValidate(tenantContext.requiredTenantId());
        Product result = productService.getProductIsValid(productId);
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "getProductValidById result = {}", result);
        return result;
    }
}
