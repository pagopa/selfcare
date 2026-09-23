package it.pagopa.selfcare.onboarding.service.impl;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.ProductId;
import it.pagopa.selfcare.tenant.TenantContext;
import it.pagopa.selfcare.onboarding.service.ProductService;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.product_json.api.ProductApi;
import org.openapi.quarkus.product_json.model.InstitutionType;
import org.openapi.quarkus.product_json.model.Origin;
import org.openapi.quarkus.product_json.model.ProductResponse;
import org.openapi.quarkus.product_json.model.RequiredDocumentResponse;
import org.openapi.quarkus.product_json.model.WorkflowTypeResponse;

import java.util.List;

@Slf4j
@ApplicationScoped
public class ProductServiceImpl implements ProductService {

    private final ProductApi productController;
    private final TenantContext tenantContext;

    public ProductServiceImpl(@RestClient ProductApi productController, TenantContext tenantContext) {
        this.productController = productController;
        this.tenantContext = tenantContext;
    }

    @Override
    public Uni<WorkflowTypeResponse> getWorkflowType(InstitutionType institutionType, Origin origin, ProductId productId) {
        log.info("Calling getWorkflowType: productId={}, institutionType={}, origin={}", productId.getValue(), institutionType, origin);
        return productController.getWorkflowType(institutionType, origin, productId.getValue(), tenantHeader(null));
    }

    @Override
    public Uni<WorkflowTypeResponse> getWorkflowType(InstitutionType institutionType, Origin origin,
                                                     ProductId productId, String tenantId) {
        return productController.getWorkflowType(institutionType, origin, productId.getValue(), tenantHeader(tenantId));
    }

    @Override
    public Uni<List<RequiredDocumentResponse>> getRequiredDocuments(ProductId productId, InstitutionType institutionType, Origin origin) {
        log.info("Calling getRequiredDocuments: productId={}, institutionType={}, origin={}", productId.getValue(), institutionType, origin);
        return productController.getRequiredDocuments(productId.getValue(), institutionType, origin, tenantHeader(null));
    }

    @Override
    public Uni<List<RequiredDocumentResponse>> getRequiredDocuments(ProductId productId,
                                                                     InstitutionType institutionType,
                                                                     Origin origin, String tenantId) {
        return productController.getRequiredDocuments(productId.getValue(), institutionType, origin, tenantHeader(tenantId));
    }

    @Override
    public Uni<Boolean> isRequiredDocuments(ProductId productId, InstitutionType institutionType, Origin origin) {
        log.info("Calling isRequiredDocumentsEnabled: productId={}, institutionType={}, origin={}", productId.getValue(), institutionType, origin);
        return productController.isRequiredDocumentsEnabled(productId.getValue(), institutionType, origin, tenantHeader(null))
                .onItem()
                .transform(response -> Boolean.parseBoolean(
                        response.getHeaderString("X-Required-Documents-Enabled")));
    }

    @Override
    public Uni<Boolean> isRequiredDocuments(ProductId productId, InstitutionType institutionType,
                                             Origin origin, String tenantId) {
        return productController.isRequiredDocumentsEnabled(productId.getValue(), institutionType, origin, tenantHeader(tenantId))
                .onItem()
                .transform(response -> Boolean.parseBoolean(
                        response.getHeaderString("X-Required-Documents-Enabled")));
    }

    @Override
    public Uni<ProductResponse> getProduct(String productId) {
        log.info("Calling getProductById: productId={}", productId);
        return productController.getProductById(productId, tenantHeader(null));
    }

    @Override
    public Uni<ProductResponse> getProduct(String productId, String tenantId) {
        return productController.getProductById(productId, tenantHeader(tenantId));
    }

    @Override
    public Uni<ProductResponse> getValidProduct(String productId) {
        log.info("Calling getValidProductById: productId={}", productId);
        return productController.getValidProductById(productId, tenantHeader(null));
    }

    @Override
    public Uni<ProductResponse> getValidProduct(String productId, String tenantId) {
        log.info("Calling getValidProductById: productId={}, tenantId={}", productId, tenantId);
        return productController.getValidProductById(productId, tenantHeader(tenantId));
    }

    @Override
    public Uni<Integer> getProductExpirationDays(String productId) {
        log.info("Calling getProductExpirationDays: productId={}", productId);
        return productController.getProductExpirationDays(productId, tenantHeader(null))
                .onItem().transform(response -> response != null && response.getExpirationDays() != null
                        ? response.getExpirationDays()
                        : it.pagopa.selfcare.onboarding.service.util.ProductConfigUtils.DEFAULT_EXPIRATION_DAYS);
    }

    @Override
    public Uni<Integer> getProductExpirationDays(String productId, String tenantId) {
        return productController.getProductExpirationDays(productId, tenantHeader(tenantId))
                .onItem().transform(response -> response != null && response.getExpirationDays() != null
                        ? response.getExpirationDays()
                        : it.pagopa.selfcare.onboarding.service.util.ProductConfigUtils.DEFAULT_EXPIRATION_DAYS);
    }

    private String tenantHeader(String tenantId) {
        if (tenantId != null && !tenantId.isBlank()) {
            return tenantId;
        }
        return tenantContext.isInitialized() ? tenantContext.getTenantId() : null;
    }
}
