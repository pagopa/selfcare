package it.pagopa.selfcare.external_api.model.onboarding;

import com.fasterxml.jackson.annotation.JsonInclude;
import it.pagopa.selfcare.commons.base.security.PartyRole;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.external_api.model.institution.RootParent;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OnboardedInstitutionInfo {
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
    private String userMailUuid;
    private String city;
    private String country;
    private String county;
    private PaymentServiceProvider paymentServiceProvider;
    private String supportEmail;
    private String supportPhone;
    private String rea;
    private String shareCapital;
    private String businessRegisterPlace;
    private DataProtectionOfficer dataProtectionOfficer;
    private RootParent rootParent;
}