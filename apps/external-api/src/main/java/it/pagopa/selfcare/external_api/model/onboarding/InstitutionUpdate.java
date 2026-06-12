package it.pagopa.selfcare.external_api.model.onboarding;

import it.pagopa.selfcare.external_api.model.institution.GPUData;
import it.pagopa.selfcare.external_api.model.institution.Payment;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.external_api.model.institution.GeographicTaxonomy;
import lombok.Data;

import java.util.List;

@Data
public class InstitutionUpdate {

    private InstitutionType institutionType;
    private String description;
    private String digitalAddress;
    private String address;
    private String city;
    private String county;
    private String country;
    private String taxCode;
    private String zipCode;
    private PaymentServiceProvider paymentServiceProvider;
    private DataProtectionOfficer dataProtectionOfficer;
    private GPUData gpuData;
    private Payment payment;
    private List<GeographicTaxonomy> geographicTaxonomies;
    private String rea;
    private String shareCapital;
    private String businessRegisterPlace;
    private String supportEmail;
    private String supportPhone;
    private Boolean imported;
    private List<String> atecoCodes;
    private String legalForm;
}
