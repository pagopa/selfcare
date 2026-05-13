package it.pagopa.selfcare.external_api.model.institution;

import it.pagopa.selfcare.external_api.model.onboarding.Billing;
import it.pagopa.selfcare.external_api.model.onboarding.DataProtectionOfficer;
import it.pagopa.selfcare.external_api.model.onboarding.PaymentServiceProvider;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(of = "id")
public class Institution {

    private String id;
    private String externalId;
    private String origin;
    private String originId;
    private String description;
    private String institutionType;
    private String digitalAddress;
    private String address;
    private String zipCode;
    private String taxCode;
    private String city;
    private String county;
    private String country;
    private String istatCode;
    private Billing billing;
    private List<Onboarding> onboarding;
    private List<GeographicTaxonomy> geographicTaxonomies;
    private List<Attribute> attributes;
    private PaymentServiceProvider paymentServiceProvider;
    private DataProtectionOfficer dataProtectionOfficer;
    private GPUData gpuData;
    private String rea;
    private String shareCapital;
    private String businessRegisterPlace;
    private String supportEmail;
    private String supportPhone;
    private boolean imported;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private String subunitCode;
    private String subunitType;
    private String rootParentId;
    private String parentDescription;
    private PaAttributes paAttributes;
    private boolean delegation;
    private String aooParentCode;
    private CompanyInformations companyInformations;
    private AssistanceContacts assistanceContacts;
}
