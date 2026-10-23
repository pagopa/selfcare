package it.pagopa.selfcare.onboarding.service.helper;

import static it.pagopa.selfcare.onboarding.constants.CustomError.DEFAULT_ERROR;
import static it.pagopa.selfcare.onboarding.constants.CustomError.UNABLE_TO_COMPLETE_THE_ONBOARDING_FOR_INSTITUTION_FOR_PRODUCT_DISMISSED;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.WorkflowType;
import it.pagopa.selfcare.onboarding.entity.Institution;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.registry.RegistryResourceFactory;
import it.pagopa.selfcare.onboarding.exception.InstitutionTypeNotAllowedException;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.exception.OnboardingNotAllowedException;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.service.ProductService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.openapi.quarkus.product_json.model.OnboardingChannel;
import org.openapi.quarkus.product_json.model.OnboardingDecisionReason;
import org.openapi.quarkus.product_json.model.OnboardingDecisionRequest;
import org.openapi.quarkus.product_json.model.OnboardingDecisionResponse;
import org.openapi.quarkus.product_json.model.Origin;

/**
 * Asks product-ms whether an onboarding is admitted. product-ms owns every admission rule (institution type and
 * origin coherence, pairs admitted by the product, self-service availability, workflow resolution and workflow
 * allowed for the origin): this helper only translates the onboarding into the request and the denial into the
 * onboarding-ms exceptions, so that a denied onboarding is always a 400.
 */
@Slf4j
@ApplicationScoped
public class OnboardingAdmissionHelper {

    static final String NOT_ALLOWED_INSTITUTION_TYPE =
            "institution with institution type %s and origin %s is not allowed to onboard product %s";

    private static final Set<OnboardingDecisionReason> INSTITUTION_TYPE_REASONS = EnumSet.of(
            OnboardingDecisionReason.INSTITUTION_TYPE_NOT_ALLOWED,
            OnboardingDecisionReason.SELF_ONBOARDING_NOT_ALLOWED);

    @Inject
    ProductService productService;

    /**
     * Decides the onboarding and, when allowed, sets the workflow type returned by product-ms and applies the
     * flow-specific settings.
     *
     * @param channel      {@link OnboardingChannel#SELF_SERVICE} for requests made by the institution,
     *                     {@link OnboardingChannel#MANAGED} for the flows operated by PagoPA
     * @param workflowType workflow imposed by the flow, {@code null} to let product-ms resolve it
     * @param onAdmitted   settings applied to the onboarding with the allowed decision
     * @return the admission, or a failure with {@link InstitutionTypeNotAllowedException} or
     *         {@link InvalidRequestException} when denied, {@link OnboardingNotAllowedException} when the product is
     *         not valid
     */
    public Uni<OnboardingAdmission> admit(Onboarding onboarding, OnboardingChannel channel, WorkflowType workflowType,
                                          Consumer<OnboardingDecisionResponse> onAdmitted) {
        Institution institution = onboarding.getInstitution();
        InstitutionType institutionType = institution.getInstitutionType();
        Origin origin = Origin.valueOf(RegistryResourceFactory.effectiveOrigin(institution.getOrigin()).name());
        if (Objects.isNull(institutionType)) {
            return Uni.createFrom().failure(new InstitutionTypeNotAllowedException(
                    String.format(NOT_ALLOWED_INSTITUTION_TYPE, null, origin, onboarding.getProductId())));
        }

        OnboardingDecisionRequest request = new OnboardingDecisionRequest()
                .institutionType(org.openapi.quarkus.product_json.model.InstitutionType.valueOf(institutionType.name()))
                .origin(origin)
                .channel(channel)
                .aggregator(Boolean.TRUE.equals(onboarding.getIsAggregator()))
                .workflowType(Objects.isNull(workflowType) ? null
                        : org.openapi.quarkus.product_json.model.WorkflowType.valueOf(workflowType.name()));

        return productService.getOnboardingDecision(onboarding.getProductId(), request, onboarding.getTenantId())
                .onFailure(ResourceNotFoundException.class).transform(ex -> new OnboardingNotAllowedException(
                        String.format(UNABLE_TO_COMPLETE_THE_ONBOARDING_FOR_INSTITUTION_FOR_PRODUCT_DISMISSED.getMessage(),
                                institution.getTaxCode(), onboarding.getProductId()),
                        DEFAULT_ERROR.getCode()))
                .onItem().transformToUni(decision -> Boolean.TRUE.equals(decision.getAllowed())
                        ? Uni.createFrom().item(apply(onboarding,
                                new OnboardingAdmission(channel, workflowType, institutionType, onAdmitted, decision)))
                        : Uni.createFrom().failure(denied(decision)));
    }

    /**
     * Decides the onboarding again when the registry step changed the institution type it was admitted for
     * (e.g. imported IPA institutions take the type of their IPA category); otherwise returns the same admission.
     */
    public Uni<OnboardingAdmission> confirm(Onboarding onboarding, OnboardingAdmission admission) {
        if (admission.institutionType() == onboarding.getInstitution().getInstitutionType()) {
            return Uni.createFrom().item(admission);
        }
        log.info("Institution type changed from {} to {} after the registry step: deciding the onboarding again",
                admission.institutionType(), onboarding.getInstitution().getInstitutionType());
        return admit(onboarding, admission.channel(), admission.workflowType(), admission.onAdmitted());
    }

    private static OnboardingAdmission apply(Onboarding onboarding, OnboardingAdmission admission) {
        onboarding.setWorkflowType(WorkflowType.valueOf(admission.decision().getWorkflowType().name()));
        admission.onAdmitted().accept(admission.decision());
        log.info("Onboarding admitted with workflowType={} for institution: {}",
                onboarding.getWorkflowType(), onboarding.getInstitution().getDescription());
        return admission;
    }

    private static InvalidRequestException denied(OnboardingDecisionResponse decision) {
        log.warn("Onboarding denied by product-ms: reason={}, message={}", decision.getReason(), decision.getMessage());
        return INSTITUTION_TYPE_REASONS.contains(decision.getReason())
                ? new InstitutionTypeNotAllowedException(decision.getMessage())
                : new InvalidRequestException(decision.getMessage());
    }
}
