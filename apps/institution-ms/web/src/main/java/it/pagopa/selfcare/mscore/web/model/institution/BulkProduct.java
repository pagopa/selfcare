package it.pagopa.selfcare.mscore.web.model.institution;

import com.fasterxml.jackson.annotation.JsonInclude;
import it.pagopa.selfcare.mscore.constant.RelationshipState;
import it.pagopa.selfcare.mscore.web.util.DecryptIfUuid;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import lombok.Data;

import jakarta.validation.constraints.NotNull;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BulkProduct {

    @NotNull
    private String product;
    @NotNull
    private BillingResponse billing;
    private String pricingPlan;
    private RelationshipState status;
    private InstitutionType institutionType;
    private String origin;
    @DecryptIfUuid
    private String originId;

}
