package it.pagopa.selfcare.onboarding.service.impl;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.client.model.OriginResult;
import it.pagopa.selfcare.onboarding.client.model.Product;
import it.pagopa.selfcare.onboarding.client.model.ProductStatus;
import it.pagopa.selfcare.onboarding.client.model.RequiredDocumentModel;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.mapper.ProductMapper;
import it.pagopa.selfcare.onboarding.service.ProductService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.product_json.api.ProductApi;
import org.openapi.quarkus.product_json.model.Origin;
import org.openapi.quarkus.product_json.model.ProductResponse;
import org.openapi.quarkus.product_json.model.RequiredDocumentResponse;
import org.owasp.encoder.Encode;

import java.util.List;
import java.util.Objects;

@Slf4j
@ApplicationScoped
public class ProductServiceImpl implements ProductService {

    static final String HEADER_REQUIRED_DOCUMENTS_ENABLED = "X-Required-Documents-Enabled";

    // No tenantId query param: product-ms resolves the tenant from the propagated X-Tenant-Id header
    private static final String TENANT_FROM_HEADER = null;

    private final ProductApi productApi;
    private final ProductMapper productMapper;

    public ProductServiceImpl(@RestClient ProductApi productApi, ProductMapper productMapper) {
        this.productApi = productApi;
        this.productMapper = productMapper;
    }

    @Override
    public Uni<OriginResult> getOrigins(String tenantId, String productId) {
        log.trace("getOrigins start");
        String productIdSanitized = Encode.forJava(productId);
        return productApi.getProductOriginsById(productIdSanitized, Encode.forJava(tenantId))
                .map(origins -> productMapper.toOriginResult(Objects.requireNonNull(origins)))
                .invoke(result -> {
                    log.debug("getOrigins size = {}", result.getOrigins().size());
                    log.trace("getOrigins end");
                });
    }

    @Override
    public List<RequiredDocumentModel> getRequiredDocuments(String tenantId, String productId, String institutionType, String origin) {
        log.trace("getRequiredDocuments start");
        String tenant = Encode.forJava(tenantId);
        String product = Encode.forJava(productId);
        org.openapi.quarkus.product_json.model.InstitutionType type = parseInstitutionType(Encode.forJava(institutionType));
        Origin documentOrigin = parseOrigin(Encode.forJava(origin));
        List<RequiredDocumentResponse> response = productApi.getRequiredDocuments(product, type, documentOrigin, tenant).await().indefinitely();
        List<RequiredDocumentModel> result = productMapper.toRequiredDocumentModelList(Objects.requireNonNull(response));
        log.debug("getRequiredDocuments size = {}", result.size());
        log.trace("getRequiredDocuments end");
        return result;
    }

    @Override
    public Uni<Boolean> isRequiredDocumentsEnabled(String tenantId, String productId, String institutionType, String origin) {
        log.trace("isRequiredDocumentsEnabled start");
        String tenant = Encode.forJava(tenantId);
        String product = Encode.forJava(productId);
        org.openapi.quarkus.product_json.model.InstitutionType type = parseInstitutionType(Encode.forJava(institutionType));
        Origin documentOrigin = parseOrigin(Encode.forJava(origin));
        return productApi.isRequiredDocumentsEnabled(product, type, documentOrigin, tenant)
                .map(response -> {
                    try (response) {
                        return Boolean.parseBoolean(response.getHeaderString(HEADER_REQUIRED_DOCUMENTS_ENABLED));
                    }
                })
                .invoke(result -> {
                    log.debug("isRequiredDocumentsEnabled result = {}", result);
                    log.trace("isRequiredDocumentsEnabled end");
                });
    }

    @Override
    public Product getProduct(String id, InstitutionType institutionType) {
        log.trace("getProduct start");
        log.debug("getProduct id = {}, institutionType = {}", Encode.forJava(id), institutionType);
        Objects.requireNonNull(id, "ProductId is required");
        ProductResponse response = productApi.getProductById(Encode.forJava(id), TENANT_FROM_HEADER).await().indefinitely();
        Product product = productMapper.toProduct(response);
        log.trace("getProduct end");
        return product;
    }

    @Override
    public Product getProductValid(String id) {
        log.trace("getProductValid start");
        log.debug("getProductValid id = {}", Encode.forJava(id));
        Objects.requireNonNull(id, "ProductId is required");
        ProductResponse response = productApi.getValidProductById(Encode.forJava(id), TENANT_FROM_HEADER).await().indefinitely();
        Product product = productMapper.toProduct(response);
        log.trace("getProductValid end");
        return product;
    }

    @Override
    public Uni<List<Product>> getProducts(boolean rootOnly) {
        log.trace("getProducts start");
        return productApi.getProducts(rootOnly, true, TENANT_FROM_HEADER)
                .map(response -> Objects.requireNonNull(response).stream()
                        .map(productMapper::toProduct)
                        .filter(product -> ProductStatus.ACTIVE.equals(product.getStatus()))
                        .toList())
                .invoke(result -> {
                    log.debug("getProducts size = {}", result.size());
                    log.trace("getProducts end");
                });
    }

    @Override
    public boolean isProductEnabled(String productId) {
        return getValidProduct(productId).isEnabled();
    }

    @Override
    public boolean verifyAllowedByInstitutionTaxCode(String productId, String institutionTaxCode) {
        List<String> allowedInstitutionTaxCodes = getValidProduct(productId).getAllowedInstitutionTaxCode();
        String taxCode = Encode.forJava(institutionTaxCode);
        return allowedInstitutionTaxCodes != null && allowedInstitutionTaxCodes.stream()
                .anyMatch(allowedTaxCode -> allowedTaxCode.equalsIgnoreCase(taxCode));
    }

    private Product getValidProduct(String productId) {
        String id = Encode.forJava(productId);
        ProductResponse response = productApi.getValidProductById(id, TENANT_FROM_HEADER).await().indefinitely();
        return productMapper.toProduct(Objects.requireNonNull(response));
    }

    // The downstream contract is strict, unlike the case-insensitive generated fromString
    private static org.openapi.quarkus.product_json.model.InstitutionType parseInstitutionType(String value) {
        for (org.openapi.quarkus.product_json.model.InstitutionType candidate : org.openapi.quarkus.product_json.model.InstitutionType.values()) {
            if (candidate.value().equals(value)) {
                return candidate;
            }
        }
        throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }

    private static Origin parseOrigin(String value) {
        for (Origin candidate : Origin.values()) {
            if (candidate.value().equals(value)) {
                return candidate;
            }
        }
        throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
}
