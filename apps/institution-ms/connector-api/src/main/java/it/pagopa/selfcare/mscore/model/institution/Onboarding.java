package it.pagopa.selfcare.mscore.model.institution;

import it.pagopa.selfcare.mscore.constant.RelationshipState;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.time.OffsetDateTime;

@Data
@FieldNameConstants(asEnum = true)
public class Onboarding {

    private String productId;
    private String tokenId;
    private RelationshipState status;
    private String contract;
    private String pricingPlan;
    private Billing billing;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime closedAt;
    private Boolean isAggregator;
    private InstitutionType institutionType;
    private String origin;
    private String originId;

}
