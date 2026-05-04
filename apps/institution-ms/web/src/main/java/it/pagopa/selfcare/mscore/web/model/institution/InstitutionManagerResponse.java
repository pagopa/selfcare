package it.pagopa.selfcare.mscore.web.model.institution;

import it.pagopa.selfcare.mscore.constant.RelationshipState;
import it.pagopa.selfcare.mscore.web.model.onboarding.ProductInfo;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class InstitutionManagerResponse {
    private String id;
    private String from;
    private String to;
    private String role;
    private ProductInfo product;
    private RelationshipState state;
    private String pricingPlan;
    private InstitutionUpdateResponse institutionUpdate;
    private BillingResponse billing;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
