package it.pagopa.selfcare.external_api.model.onboarding;

import it.pagopa.selfcare.external_api.model.institution.RootParent;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import lombok.Data;

import java.util.Collection;
import java.util.UUID;

@Data
public class OnboardedInstitutionResource {

    private UUID id;
    private String description;
    private String externalId;
    private String originId;
    private InstitutionType institutionType;
    private String digitalAddress;
    private String status;
    private String address;
    private String zipCode;
    private String taxCode;
    private String taxCodeInvoicing;
    private String origin;
    private Collection<String> userProductRoles;
    private String recipientCode;
    private String rea;
    private String shareCapital;
    private String businessRegisterPlace;
    private String supportEmail;
    private String supportPhone;
    private PaymentServiceProvider paymentServiceProvider;
    private DataProtectionOfficer dataProtectionOfficer;
    private String subunitCode;
    private String subunitType;
    private RootParent rootParent;
    private String aooParentCode;
    private String country;
    private String county;
    private String city;

}
