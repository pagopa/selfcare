package it.pagopa.selfcare.onboarding.connector.rest.model;

import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.connector.model.institutions.Attribute;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.DataProtectionOfficer;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.GeographicTaxonomy;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.OnboardingData;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.PaymentServiceProvider;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class InstitutionSeed {

    public InstitutionSeed(OnboardingData onboardingData) {
        description = onboardingData.getInstitutionUpdate().getDescription();
        digitalAddress = onboardingData.getInstitutionUpdate().getDigitalAddress();
        address = onboardingData.getInstitutionUpdate().getAddress();
        zipCode = onboardingData.getInstitutionUpdate().getZipCode();
        taxCode = onboardingData.getInstitutionUpdate().getTaxCode();
        institutionType = onboardingData.getInstitutionType();
        attributes = List.of();
        if(onboardingData.getLocation() != null) {
            city = onboardingData.getLocation().getCity();
            county = onboardingData.getLocation().getCounty();
            country = onboardingData.getLocation().getCountry();
        }
        paymentServiceProvider = onboardingData.getInstitutionUpdate().getPaymentServiceProvider();
        dataProtectionOfficer = onboardingData.getInstitutionUpdate().getDataProtectionOfficer();
        geographicTaxonomies = onboardingData.getInstitutionUpdate().getGeographicTaxonomies();
        rea = onboardingData.getInstitutionUpdate().getRea();
        shareCapital = onboardingData.getInstitutionUpdate().getShareCapital();
        businessRegisterPlace = onboardingData.getInstitutionUpdate().getBusinessRegisterPlace();
        supportEmail = onboardingData.getInstitutionUpdate().getSupportEmail();
        supportPhone = onboardingData.getInstitutionUpdate().getSupportPhone();
    }


    private String description;
    private String digitalAddress;
    private String address;
    private String zipCode;
    private String taxCode;
    private String city;
    private String county;
    private String country;
    private InstitutionType institutionType;
    private List<Attribute> attributes;
    private PaymentServiceProvider paymentServiceProvider;
    private DataProtectionOfficer dataProtectionOfficer;
    private List<GeographicTaxonomy> geographicTaxonomies;
    private String rea;
    private String shareCapital;
    private String businessRegisterPlace;
    private String supportEmail;
    private String supportPhone;

}
