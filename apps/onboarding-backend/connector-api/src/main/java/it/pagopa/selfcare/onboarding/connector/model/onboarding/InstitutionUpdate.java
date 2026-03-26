package it.pagopa.selfcare.onboarding.connector.model.onboarding;

import it.pagopa.selfcare.onboarding.common.InstitutionType;
import lombok.Data;

import java.util.List;


@Data
public class InstitutionUpdate {

    private String id;
    private InstitutionType institutionType;
    private String description;
    private String parentDescription;
    private String digitalAddress;
    private String address;
    private String taxCode;
    private String zipCode;
    private String origin;
    private String originId;
    private String legalForm;
    private PaymentServiceProvider paymentServiceProvider;
    private DataProtectionOfficer dataProtectionOfficer;
    private GPUData gpuData;
    private List<GeographicTaxonomy> geographicTaxonomies;
    private String rea;
    private String shareCapital;
    private String businessRegisterPlace;
    private String supportEmail;
    private String supportPhone;
    private Boolean imported;
    private AdditionalInformations additionalInformations;
}
