package it.pagopa.selfcare.external_api.model.onboarding;

import com.fasterxml.jackson.annotation.JsonInclude;
import it.pagopa.selfcare.commons.base.security.PartyRole;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OnboardedInstitutionResponse {

    private String id;
    private String externalId;
    private String originId;
    private String description;
    private String origin;
    private InstitutionType institutionType;
    private String digitalAddress;
    private String address;
    private String zipCode;
    private String taxCode;
    private String pricingPlan;
    private Billing billing;
    private String state;
    private PartyRole role;
    private ProductInfo productInfo;
    private String subunitCode;
    private String subunitType;
    private String aooParentCode;
    private String userEmail;
    private String userMobilePhone;
    private String userTelephone;
}