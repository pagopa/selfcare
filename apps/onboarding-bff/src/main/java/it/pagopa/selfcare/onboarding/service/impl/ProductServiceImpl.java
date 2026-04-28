package it.pagopa.selfcare.onboarding.service.impl;

import it.pagopa.selfcare.onboarding.service.*;

import it.pagopa.selfcare.onboarding.client.model.OriginEntry;
import it.pagopa.selfcare.onboarding.client.model.OriginResult;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.Origin;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.entity.ProductStatus;
import it.pagopa.selfcare.product.exception.ProductNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.product_json.api.ProductApi;
import org.openapi.quarkus.product_json.model.ProductCreateRequestInstitutionOriginsInner;
import org.openapi.quarkus.product_json.model.ProductOriginResponse;
import org.owasp.encoder.Encode;

import java.util.List;
import java.util.Objects;

@Slf4j
@ApplicationScoped
public class ProductServiceImpl implements ProductService {

    private final ProductApi productApi;
    private final it.pagopa.selfcare.product.service.ProductService sdkProductService;

    public ProductServiceImpl(@RestClient ProductApi productApi,
                              it.pagopa.selfcare.product.service.ProductService sdkProductService) {
        this.productApi = productApi;
        this.sdkProductService = sdkProductService;
    }

    @Override
    public OriginResult getOrigins(String productId) {
        log.trace("getOrigins start");
        String productIdSanitized = Encode.forJava(productId);
        ProductOriginResponse origins = productApi.getProductOriginsById(productIdSanitized).await().indefinitely();
        List<OriginEntry> entries = origins == null || origins.getOrigins() == null
            ? null
            : origins.getOrigins().stream().map(this::toOriginEntry).toList();
        OriginResult originResult = OriginResult.builder().origins(entries).build();
        log.debug("getOrigins size = {}", originResult.getOrigins().size());
        log.trace("getOrigins end");
        return originResult;
    }

    @Override
    public Product getProduct(String id, InstitutionType institutionType) {
        log.trace("getProduct start");
        String idSanitized = Encode.forJava(id);
        log.debug("getProduct id = {}, institutionType = {}", idSanitized, institutionType);
        Objects.requireNonNull(id, "ProductId is required");
        try {
            Product product = sdkProductService.getProduct(id);
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
        Product product = sdkProductService.getProductIsValid(id);
        log.debug("getProductValid result = {}", product);
        log.trace("getProductValid end");
        return product;
    }

    @Override
    public List<Product> getProducts(boolean rootOnly) {
        log.trace("getProducts start");
        List<Product> products = sdkProductService.getProducts(rootOnly, true);
        List<Product> activeProducts = products.stream()
                .filter(product -> ProductStatus.ACTIVE.equals(product.getStatus()))
                .toList();
        log.debug("getProducts result = {}", activeProducts);
        log.trace("getProducts end");
        return activeProducts;
    }

    private OriginEntry toOriginEntry(ProductCreateRequestInstitutionOriginsInner source) {
        OriginEntry target = new OriginEntry();
        if (source.getInstitutionType() != null) {
            target.setInstitutionType(OriginEntry.InstitutionType.valueOf(source.getInstitutionType().value()));
        }
        if (source.getOrigin() != null) {
            target.setOrigin(Origin.valueOf(source.getOrigin().value()));
        }
        target.setLabelKey(source.getLabelKey());
        return target;
    }
}
