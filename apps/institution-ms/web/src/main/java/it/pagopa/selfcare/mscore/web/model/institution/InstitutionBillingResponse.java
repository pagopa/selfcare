package it.pagopa.selfcare.mscore.web.model.institution;

import it.pagopa.selfcare.mscore.constant.Origin;
import it.pagopa.selfcare.mscore.web.util.DecryptIfUuid;
import lombok.Data;

@Data
public class InstitutionBillingResponse {
    private String institutionId;
    private String externalId;
    private Origin origin;
    private String originId;
    private String description;
    private String institutionType;
    private String digitalAddress;
    private String address;
    private String zipCode;
    @DecryptIfUuid
    private String taxCode;
    private String pricingPlan;
    private BillingResponse billing;

    private String subunitCode;
    private String subunitType;
    private String aooParentCode;
}
