package it.pagopa.selfcare.external_api.model.institution;

import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.external_api.model.onboarding.DataProtectionOfficer;
import it.pagopa.selfcare.external_api.model.onboarding.PaymentServiceProvider;
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
    private Boolean imported;
    private String country;
    private String county;
    private String city;
    private String subunitCode;
    private String subunitType;
    private String parentDescription;
    private String aooParentCode;
    private CompanyInformations companyInformations;
    private AssistanceContacts assistanceContacts;
}
