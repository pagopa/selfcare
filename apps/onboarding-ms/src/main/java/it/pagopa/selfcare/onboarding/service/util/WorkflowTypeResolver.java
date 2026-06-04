package it.pagopa.selfcare.onboarding.service.util;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.WorkflowType;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.service.ProductMsService;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.service.ProductService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.openapi.quarkus.product_json.model.WorkflowTypeResponse;

import java.util.Optional;

@ApplicationScoped
public class WorkflowTypeResolver {

    @Inject
    ProductService productService;

    @Inject
    ProductMsService productMsService;

    public Uni<WorkflowType> resolve(Onboarding onboarding) {
        Product product = productService.getProductIsValid(onboarding.getProductId());

        Optional<WorkflowType> priorityOverride = resolveByPriority(onboarding, product);
        if (priorityOverride.isPresent()) {
            return Uni.createFrom().item(priorityOverride.get());
        }

        return resolveFromProductApi(onboarding);
    }

    // ─── Level 1: Global priority overrides ─────────────────────────────────────

    private Optional<WorkflowType> resolveByPriority(Onboarding onboarding, Product product) {
        return resolvePtOverride(onboarding)
                .or(() -> resolveAggregatorOverride(onboarding))
                .or(() -> resolveCountersignatureOverride(product))
                .or(() -> resolveSubProductOverride(product));
    }

    private Optional<WorkflowType> resolvePtOverride(Onboarding onboarding) {
        if (InstitutionType.PT.equals(onboarding.getInstitution().getInstitutionType())) {
            return Optional.of(WorkflowType.FOR_APPROVE_PT);
        }
        return Optional.empty();
    }

    private Optional<WorkflowType> resolveAggregatorOverride(Onboarding onboarding) {
        if (Boolean.TRUE.equals(onboarding.getIsAggregator())) {
            return Optional.of(WorkflowType.CONTRACT_REGISTRATION_AGGREGATOR);
        }
        return Optional.empty();
    }

    private Optional<WorkflowType> resolveCountersignatureOverride(Product product) {
        if (product.getSigningConfiguration() != null
                && product.getSigningConfiguration().getRequiredSignatures() > 1) {
            return Optional.of(WorkflowType.CONTRACT_WITH_COUNTERSIGNATURE);
        }
        return Optional.empty();
    }

    private Optional<WorkflowType> resolveSubProductOverride(Product product) {
        if (product.getParentId() != null) {
            return Optional.of(WorkflowType.CONTRACT_REGISTRATION);
        }
        return Optional.empty();
    }

    // ─── Level 2: Resolve via Product MS REST API ────────────────────────────────

    private Uni<WorkflowType> resolveFromProductApi(Onboarding onboarding) {
        InstitutionType institutionType = onboarding.getInstitution().getInstitutionType();
        it.pagopa.selfcare.onboarding.common.Origin origin = onboarding.getInstitution().getOrigin();

        org.openapi.quarkus.product_json.model.InstitutionType apiInstitutionType =
                institutionType != null
                        ? org.openapi.quarkus.product_json.model.InstitutionType.valueOf(institutionType.name())
                        : null;

        org.openapi.quarkus.product_json.model.Origin apiOrigin =
                origin != null
                        ? org.openapi.quarkus.product_json.model.Origin.valueOf(origin.name())
                        : null;

        return productMsService.getWorkflowType(apiInstitutionType, apiOrigin, onboarding.getProductId())
                .onItem().transform(this::mapWorkflowType)
                .onFailure().transform(ex -> new IllegalStateException(String.format(
                        "Failed to resolve workflowType from Product MS for product '%s', institutionType '%s', origin '%s': %s",
                        onboarding.getProductId(), institutionType, origin, ex.getMessage()), ex));
    }

    private WorkflowType mapWorkflowType(WorkflowTypeResponse response) {
        if (response == null || response.getWorkflowType() == null) {
            throw new IllegalStateException("Product MS returned a null workflowType in the response.");
        }
        return WorkflowType.valueOf(response.getWorkflowType().name());
    }
}
