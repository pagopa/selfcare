package it.pagopa.selfcare.mscore.web.model.institution;

import it.pagopa.selfcare.onboarding.common.InstitutionType;
import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import java.time.OffsetDateTime;

@Data
public class InstitutionOnboardingRequest {

    @NotEmpty(message = "productId is required")
    private String productId;

    private String tokenId;
    private String contractPath;
    private String pricingPlan;
    private BillingRequest billing;
    private OffsetDateTime activatedAt;
    private Boolean isAggregator;
    private InstitutionType institutionType;
    private String origin;
    private String originId;

}
