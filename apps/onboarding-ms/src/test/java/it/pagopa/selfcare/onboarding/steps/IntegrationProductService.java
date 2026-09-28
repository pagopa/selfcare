package it.pagopa.selfcare.onboarding.steps;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.ProductId;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.service.ProductService;
import it.pagopa.selfcare.onboarding.service.util.ProductConfigUtils;
import it.pagopa.selfcare.tenant.TenantContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.openapi.quarkus.product_json.model.*;

@Alternative
@ApplicationScoped
public class IntegrationProductService implements ProductService {
    private final Map<String, Map<String, ProductResponse>> catalog = IntegrationProductFixtures.load();

    @Inject TenantContext tenantContext;

    @Override
    public Uni<ProductResponse> getProduct(String productId) {
        return getProduct(productId, null);
    }

    @Override
    public Uni<ProductResponse> getProduct(String productId, String tenantId) {
        String tenant = tenant(tenantId);
        ProductResponse product = catalog.getOrDefault(tenant, Map.of()).get(productId);
        return product == null
                ? Uni.createFrom().failure(new ResourceNotFoundException("Product not found with id: " + productId))
                : Uni.createFrom().item(product);
    }

    @Override
    public Uni<ProductResponse> getValidProduct(String productId) {
        return getValidProduct(productId, null);
    }

    @Override
    public Uni<ProductResponse> getValidProduct(String productId, String tenantId) {
        return getProduct(productId, tenantId).chain(product -> {
            if (!isValid(product)) {
                return Uni.createFrom().failure(new ResourceNotFoundException("Product not found with id: " + productId));
            }
            if (product.getParentId() == null) {
                return Uni.createFrom().item(product);
            }
            return getProduct(product.getParentId(), tenantId).chain(parent -> isValid(parent)
                    ? Uni.createFrom().item(product)
                    : Uni.createFrom().failure(new ResourceNotFoundException("Product not found with id: " + productId)));
        });
    }

    @Override
    public Uni<WorkflowTypeResponse> getWorkflowType(InstitutionType type, Origin origin, ProductId id) {
        return getWorkflowType(type, origin, id, null);
    }

    @Override
    public Uni<WorkflowTypeResponse> getWorkflowType(InstitutionType type, Origin origin, ProductId id, String tenantId) {
        return getValidProduct(id.getValue(), tenantId).chain(product ->
                Objects.requireNonNullElse(product.getWorkflowRules(), List.<WorkflowRule>of()).stream()
                        .filter(rule -> rule.getInstitutionType() == type && rule.getOrigin() == origin)
                        .findFirst()
                        .map(rule -> Uni.createFrom().item(new WorkflowTypeResponse().workflowType(rule.getWorkflowType())))
                        .orElseGet(() -> Uni.createFrom().failure(new ResourceNotFoundException(
                                "No workflowRule found for product " + id.getValue()))));
    }

    @Override
    public Uni<List<RequiredDocumentResponse>> getRequiredDocuments(ProductId id, InstitutionType type, Origin origin) {
        return getRequiredDocuments(id, type, origin, null);
    }

    @Override
    public Uni<List<RequiredDocumentResponse>> getRequiredDocuments(ProductId id, InstitutionType type,
                                                                  Origin origin, String tenantId) {
        return getProduct(id.getValue(), tenantId).map(product ->
                Objects.requireNonNullElse(product.getRequiredDocuments(), List.<RequiredDocument>of()).stream()
                        .filter(document -> document.getFilter() != null
                                && document.getFilter().getInstitutionType() != null
                                && document.getFilter().getInstitutionType().contains(type)
                                && document.getFilter().getOrigin() != null
                                && document.getFilter().getOrigin().contains(origin))
                        .map(document -> new RequiredDocumentResponse().id(document.getId())
                                .name(document.getName()).labelKey(document.getLabelKey())
                                .required(document.getRequired()).mimeType(document.getMimeType())
                                .maxDocumentsRequired(document.getMaxDocumentsRequired())
                                .storageOrigin(document.getStorageOrigin()))
                        .toList());
    }

    @Override
    public Uni<Boolean> isRequiredDocuments(ProductId id, InstitutionType type, Origin origin) {
        return isRequiredDocuments(id, type, origin, null);
    }

    @Override
    public Uni<Boolean> isRequiredDocuments(ProductId id, InstitutionType type, Origin origin, String tenantId) {
        return getRequiredDocuments(id, type, origin, tenantId).map(documents -> !documents.isEmpty());
    }

    @Override
    public Uni<Integer> getProductExpirationDays(String productId) {
        return getProductExpirationDays(productId, null);
    }

    @Override
    public Uni<Integer> getProductExpirationDays(String productId, String tenantId) {
        return getProduct(productId, tenantId).map(ProductConfigUtils::expirationDays);
    }

    private String tenant(String explicit) {
        String contextual = tenantContext != null && tenantContext.isInitialized()
                ? tenantContext.getTenantId().trim().toUpperCase(Locale.ROOT) : null;
        String resolved = explicit == null ? contextual : explicit.trim().toUpperCase(Locale.ROOT);
        if (resolved == null || !catalog.containsKey(resolved)
                || (contextual != null && !contextual.equals(resolved))) {
            throw new IllegalArgumentException("Missing, unknown or conflicting fixture tenant");
        }
        return resolved;
    }

    private static boolean isValid(ProductResponse product) {
        return product.getStatus() == ProductStatus.ACTIVE || product.getStatus() == ProductStatus.TESTING;
    }

}
