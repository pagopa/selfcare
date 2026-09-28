package it.pagopa.selfcare.onboarding.service.impl;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.ProductId;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.tenant.TenantContext;
import it.pagopa.selfcare.tenant.TenantRegistry;
import it.pagopa.selfcare.onboarding.service.ProductService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
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
    private final TenantRegistry tenantRegistry;

    public ProductServiceImpl(@RestClient ProductApi productController, TenantContext tenantContext,
                              TenantRegistry tenantRegistry) {
        this.productController = productController;
        this.tenantContext = tenantContext;
        this.tenantRegistry = tenantRegistry;
    }

    @Override
    public Uni<WorkflowTypeResponse> getWorkflowType(InstitutionType institutionType, Origin origin, ProductId productId) {
        log.info("Calling getWorkflowType: productId={}, institutionType={}, origin={}", productId.getValue(), institutionType, origin);
        return getWorkflowType(institutionType, origin, productId, null);
    }

    @Override
    public Uni<WorkflowTypeResponse> getWorkflowType(InstitutionType institutionType, Origin origin,
                                                     ProductId productId, String tenantId) {
        return mapNotFound(productController.getWorkflowType(canonicalTenant(tenantId), institutionType, origin,
                productId.getValue()), productId.getValue());
    }

    @Override
    public Uni<List<RequiredDocumentResponse>> getRequiredDocuments(ProductId productId, InstitutionType institutionType, Origin origin) {
        log.info("Calling getRequiredDocuments: productId={}, institutionType={}, origin={}", productId.getValue(), institutionType, origin);
        return getRequiredDocuments(productId, institutionType, origin, null);
    }

    @Override
    public Uni<List<RequiredDocumentResponse>> getRequiredDocuments(ProductId productId,
                                                                     InstitutionType institutionType,
                                                                     Origin origin, String tenantId) {
        return mapNotFound(productController.getRequiredDocuments(productId.getValue(), canonicalTenant(tenantId),
                institutionType, origin), productId.getValue());
    }

    @Override
    public Uni<Boolean> isRequiredDocuments(ProductId productId, InstitutionType institutionType, Origin origin) {
        log.info("Calling isRequiredDocumentsEnabled: productId={}, institutionType={}, origin={}", productId.getValue(), institutionType, origin);
        return isRequiredDocuments(productId, institutionType, origin, null);
    }

    @Override
    public Uni<Boolean> isRequiredDocuments(ProductId productId, InstitutionType institutionType,
                                             Origin origin, String tenantId) {
        return mapNotFound(productController.isRequiredDocumentsEnabled(productId.getValue(), canonicalTenant(tenantId),
                institutionType, origin), productId.getValue())
                .onItem()
                .transform(response -> {
                    try (response) {
                        return Boolean.parseBoolean(response.getHeaderString("X-Required-Documents-Enabled"));
                    }
                });
    }

    @Override
    public Uni<ProductResponse> getProduct(String productId) {
        log.info("Calling getProductById: productId={}", productId);
        return getProduct(productId, null);
    }

    @Override
    public Uni<ProductResponse> getProduct(String productId, String tenantId) {
        return mapNotFound(productController.getProductById(productId, canonicalTenant(tenantId)), productId);
    }

    @Override
    public Uni<ProductResponse> getValidProduct(String productId) {
        log.info("Calling getValidProductById: productId={}", productId);
        return getValidProduct(productId, null);
    }

    @Override
    public Uni<ProductResponse> getValidProduct(String productId, String tenantId) {
        log.info("Calling getValidProductById: productId={}, tenantId={}", productId, tenantId);
        return mapNotFound(productController.getValidProductById(productId, canonicalTenant(tenantId)), productId);
    }

    @Override
    public Uni<Integer> getProductExpirationDays(String productId) {
        log.info("Calling getProductExpirationDays: productId={}", productId);
        return getProductExpirationDays(productId, null);
    }

    @Override
    public Uni<Integer> getProductExpirationDays(String productId, String tenantId) {
        return mapNotFound(productController.getProductExpirationDays(productId, canonicalTenant(tenantId)), productId)
                .onItem().transform(response -> response != null && response.getExpirationDays() != null
                        ? response.getExpirationDays()
                        : it.pagopa.selfcare.onboarding.service.util.ProductConfigUtils.DEFAULT_EXPIRATION_DAYS);
    }

    private String canonicalTenant(String tenantId) {
        String canonicalTenant = tenantRegistry.normalizeTenantId(
                tenantId != null ? tenantId : tenantContext.requiredTenantId());
        tenantRegistry.resolve(canonicalTenant);
        if (tenantContext.isInitialized()) {
            String contextTenant = tenantRegistry.normalizeTenantId(tenantContext.getTenantId());
            tenantRegistry.resolve(contextTenant);
            if (!canonicalTenant.equals(contextTenant)) {
                throw new IllegalArgumentException("Explicit tenant does not match the current tenant context");
            }
        }
        tenantContext.setTenantId(canonicalTenant);
        return canonicalTenant;
    }

    private <T> Uni<T> mapNotFound(Uni<T> result, String productId) {
        return result.onFailure(failure -> failure instanceof WebApplicationException exception
                        && exception.getResponse() != null && exception.getResponse().getStatus() == 404)
                .transform(failure -> new ResourceNotFoundException("Product not found with id: " + productId));
    }
}
