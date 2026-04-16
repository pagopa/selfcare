package it.pagopa.selfcare.mscore.web.model.institution;

import it.pagopa.selfcare.mscore.constant.RelationshipState;
import it.pagopa.selfcare.mscore.web.util.DecryptIfUuid;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class OnboardingResponse {

    private String productId;
    private String tokenId;
    private RelationshipState status;
    private String contract;
    private String pricingPlan;
    private BillingResponse billing;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime closedAt;
    private Boolean isAggregator;
    private InstitutionType institutionType;
    private String origin;
    @DecryptIfUuid
    private String originId;

}
