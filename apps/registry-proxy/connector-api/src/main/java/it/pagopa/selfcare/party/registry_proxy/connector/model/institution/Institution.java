package it.pagopa.selfcare.party.registry_proxy.connector.model.institution;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
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
    private String ivassCode;
    private String city;
    private String county;
    private String country;
    private String istatCode;
    private Billing billing;
    private List<Onboarding> onboarding;
    private List<InstitutionGeographicTaxonomies> geographicTaxonomies;
    private List<Attributes> attributes;
    private PaymentServiceProvider paymentServiceProvider;
    private DataProtectionOfficer dataProtectionOfficer;
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
    private Boolean isTest;
    private String legalForm;

}
