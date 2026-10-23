package it.pagopa.selfcare.onboarding.service.helper;

import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.WorkflowType;
import java.util.function.Consumer;
import org.openapi.quarkus.product_json.model.OnboardingChannel;
import org.openapi.quarkus.product_json.model.OnboardingDecisionResponse;
import org.openapi.quarkus.product_json.model.ProductResponse;

/**
 * An onboarding admitted by product-ms, together with the request it was decided for, so that it can be decided
 * again when the registry step changes the institution type.
 *
 * @param channel             channel the onboarding was requested through
 * @param workflowType        workflow imposed by the flow, {@code null} when product-ms resolves it
 * @param institutionType     institution type the decision was taken for
 * @param onAdmitted          flow-specific settings applied to the onboarding for each allowed decision
 * @param decision            allowed decision returned by product-ms
 */
public record OnboardingAdmission(OnboardingChannel channel,
                                  WorkflowType workflowType,
                                  InstitutionType institutionType,
                                  Consumer<OnboardingDecisionResponse> onAdmitted,
                                  OnboardingDecisionResponse decision) {

    /** The valid product the decision was taken on. */
    public ProductResponse product() {
        return decision.getProduct();
    }
}
