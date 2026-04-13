package it.pagopa.selfcare.onboarding.client.model;

import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.client.model.Attribute;
import it.pagopa.selfcare.onboarding.client.model.DataProtectionOfficer;
import it.pagopa.selfcare.onboarding.client.model.GPUData;
import it.pagopa.selfcare.onboarding.client.model.GeographicTaxonomy;
import it.pagopa.selfcare.onboarding.client.model.PaymentServiceProvider;
import lombok.Data;

import java.util.List;

@Data
public class InstitutionResponse {

    private String id;
    private String externalId;
    private String originId;
    private String description;
    private String digitalAddress;
    private String address;
    private String zipCode;
    private String taxCode;
    private String origin;
    private String city;
    private String county;
    private String country;
    private String subunitCode;
    private String subunitType;
    private String aooParentCode;
    private InstitutionType institutionType;
    private List<Attribute> attributes;
    private PaymentServiceProvider paymentServiceProvider;
    private DataProtectionOfficer dataProtectionOfficer;
    private GPUData gpuData;
    private List<GeographicTaxonomy> geographicTaxonomies;
    private List<OnboardingResponse> onboarding;
    private String rea;
    private String shareCapital;
    private String businessRegisterPlace;
    private String supportEmail;
    private String supportPhone;
    private Boolean imported;

}
