package it.pagopa.selfcare.party.registry_proxy.connector.model.institution;

import com.fasterxml.jackson.annotation.JsonInclude;

import it.pagopa.selfcare.onboarding.common.InstitutionType;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldNameConstants(asEnum = true)
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
    private List<InstitutionGeographicTaxonomies> geographicTaxonomies;
    private List<Onboarding> onboardings;
    private String rea;
    private String shareCapital;
    private String businessRegisterPlace;
    private String supportEmail;
    private String supportPhone;
    private String ivassCode;
    private boolean imported;
    private AdditionalInformations additionalInformations;
    private Boolean delegation;
    private String parentDescription;
}
