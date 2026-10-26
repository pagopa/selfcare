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
import org.openapi.quarkus.product_json.model.OnboardingDecisionRequest;
import org.openapi.quarkus.product_json.model.OnboardingDecisionResponse;
import org.openapi.quarkus.product_json.model.Origin;
import org.openapi.quarkus.product_json.model.ProductResponse;
import org.openapi.quarkus.product_json.model.RequiredDocumentResponse;
import org.owasp.encoder.Encode;

import java.util.List;
import java.util.Objects;

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
    public Uni<OnboardingDecisionResponse> getOnboardingDecision(String productId, OnboardingDecisionRequest request,
                                                               String tenantId) {
        log.info("Calling getOnboardingDecision: productId={}, institutionType={}, origin={}, channel={}",
                Encode.forJava(String.valueOf(productId)), request.getInstitutionType(), request.getOrigin(),
                request.getChannel());
        return mapNotFound(productController.getOnboardingDecision(productId, request, canonicalTenant(tenantId)),
                productId);
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
        return mapNotFound(productController.getRequiredDocuments(productId.getValue(), institutionType, origin,
                canonicalTenant(tenantId)), productId.getValue());
    }

    @Override
    public Uni<ProductResponse> getValidProduct(String productId) {
        log.info("Calling getValidProductById: productId={}", Encode.forJava(String.valueOf(productId)));
        return getValidProduct(productId, null);
    }

    @Override
    public Uni<ProductResponse> getValidProduct(String productId, String tenantId) {
        log.info("Calling getValidProductById: productId={}, tenantId={}",
                Encode.forJava(String.valueOf(productId)), Encode.forJava(String.valueOf(tenantId)));
        return mapNotFound(productController.getValidProductById(productId, canonicalTenant(tenantId)), productId);
    }

    @Override
    public Uni<Integer> getProductExpirationDays(String productId) {
        log.info("Calling getProductExpirationDays: productId={}", Encode.forJava(String.valueOf(productId)));
        return getProductExpirationDays(productId, null);
    }

    @Override
    public Uni<Integer> getProductExpirationDays(String productId, String tenantId) {
        return mapNotFound(productController.getProductExpirationDays(productId, canonicalTenant(tenantId)), productId)
                .onItem().transform(response -> Objects.nonNull(response) && Objects.nonNull(response.getExpirationDays())
                        ? response.getExpirationDays()
                        : it.pagopa.selfcare.onboarding.service.util.ProductConfigUtils.DEFAULT_EXPIRATION_DAYS);
    }

    private String canonicalTenant(String tenantId) {
        String canonicalTenant = tenantRegistry.normalizeTenantId(
                Objects.nonNull(tenantId) ? tenantId : tenantContext.requiredTenantId());
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
                        && Objects.nonNull(exception.getResponse()) && exception.getResponse().getStatus() == 404)
                .transform(failure -> new ResourceNotFoundException("Product not found with id: " + productId));
    }
}
