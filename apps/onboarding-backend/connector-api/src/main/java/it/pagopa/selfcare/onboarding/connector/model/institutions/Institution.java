package it.pagopa.selfcare.onboarding.connector.model.institutions;

import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.*;
import lombok.Data;

import java.util.List;

@Data
public class Institution {

    private String id;
    private String externalId;
    private String originId;
    private String vatNumber;
    private String description;
    private String parentDescription;
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
    private List<InstitutionOnboarding> onboarding;
    private InstitutionType institutionType;
    private List<Attribute> attributes;
    private PaymentServiceProvider paymentServiceProvider;
    private DataProtectionOfficer dataProtectionOfficer;
    private GPUData gpuData;
    private List<GeographicTaxonomy> geographicTaxonomies;
    private CompanyInformations companyInformations;
    private AssistanceContacts assistanceContacts;
    private List<User> users;
    private String iban;
    private String recipientCode;
}
