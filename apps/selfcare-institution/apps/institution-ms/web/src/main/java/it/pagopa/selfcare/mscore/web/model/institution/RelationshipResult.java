package it.pagopa.selfcare.mscore.web.model.institution;

import com.fasterxml.jackson.annotation.JsonInclude;
import it.pagopa.selfcare.commons.base.security.PartyRole;
import it.pagopa.selfcare.mscore.constant.RelationshipState;
import it.pagopa.selfcare.mscore.web.model.onboarding.ProductInfo;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RelationshipResult {

    private String id;
    private String to;
    private String from;
    private PartyRole role;
    private String tokenId;
    private ProductInfo product;
    private RelationshipState state;
    private String pricingPlan;
    private InstitutionUpdateResponse institutionUpdate;
    private BillingResponse billing;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

}
