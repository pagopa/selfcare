package it.pagopa.selfcare.mscore.connector.dao.model.inner;

import it.pagopa.selfcare.mscore.constant.RelationshipState;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.OffsetDateTime;

@Data
@FieldNameConstants(asEnum = true)
public class OnboardingEntity {

    @Indexed(unique = true)
    private String productId;
    private RelationshipState status;
    private String tokenId;
    private String contract;
    private String pricingPlan;
    private BillingEntity billing;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime closedAt;
    private Boolean isAggregator;
    private InstitutionType institutionType;
    private String origin;
    private String originId;
}
