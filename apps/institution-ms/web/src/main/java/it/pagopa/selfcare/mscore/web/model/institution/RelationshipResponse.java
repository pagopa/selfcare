package it.pagopa.selfcare.mscore.web.model.institution;

import it.pagopa.selfcare.mscore.constant.RelationshipState;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
public class RelationshipResponse {
    private String id;
    private List<String> from;
    private String to;
    private String role;
    private String product;
    private RelationshipState state;
    private String pricingPlan;
    private InstitutionUpdateResponse institutionUpdate;
    private BillingResponse billingResponse;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
