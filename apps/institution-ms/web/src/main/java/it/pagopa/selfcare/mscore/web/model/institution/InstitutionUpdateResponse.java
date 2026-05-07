package it.pagopa.selfcare.mscore.web.model.institution;

import com.fasterxml.jackson.annotation.JsonInclude;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InstitutionUpdateResponse {
    private InstitutionType institutionType;
    private String description;
    private String digitalAddress;
    private String address;
    private String taxCode;
    private String zipCode;
    private PaymentServiceProviderResponse paymentServiceProvider;
    private DataProtectionOfficerResponse dataProtectionOfficer;
    private List<String> geographicTaxonomyCodes;
    private String rea;
    private String shareCapital;
    private String businessRegisterPlace;
    private String supportEmail;
    private String supportPhone;
    private boolean imported;
    private String city;
    private String county;
    private String country;
    private String subunitCode;
    private String subunitType;
    private String aooParentCode;
}

