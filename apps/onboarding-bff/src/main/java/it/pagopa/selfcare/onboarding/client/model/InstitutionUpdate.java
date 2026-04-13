package it.pagopa.selfcare.onboarding.client.model;

import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.client.model.AdditionalInformations;
import it.pagopa.selfcare.onboarding.client.model.DataProtectionOfficer;
import it.pagopa.selfcare.onboarding.client.model.PaymentServiceProvider;
import lombok.Data;

import java.util.List;

@Data
public class InstitutionUpdate {

    private InstitutionType institutionType;
    private String description;
    private String digitalAddress;
    private String address;
    private String taxCode;
    private String zipCode;
    private String city;
    private String county;
    private String country;
    private PaymentServiceProvider paymentServiceProvider;
    private DataProtectionOfficer dataProtectionOfficer;
    private List<String> geographicTaxonomyCodes;
    private String rea;
    private String shareCapital;
    private String businessRegisterPlace;
    private String supportEmail;
    private String supportPhone;
    private Boolean imported;
    private AdditionalInformations additionalInformations;
}
