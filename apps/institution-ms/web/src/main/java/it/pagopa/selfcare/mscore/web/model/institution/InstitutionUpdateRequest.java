package it.pagopa.selfcare.mscore.web.model.institution;

import it.pagopa.selfcare.onboarding.common.InstitutionType;
import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
public class InstitutionUpdateRequest {

    @NotNull(message = "institutionType is required")
    private InstitutionType institutionType;

    private String description;
    private String digitalAddress;
    private String address;
    private String city;
    private String county;
    private String country;
    @NotEmpty(message = "taxCode is required")
    private String taxCode;

    private String zipCode;
    private PaymentServiceProviderRequest paymentServiceProvider;
    private DataProtectionOfficerRequest dataProtectionOfficer;
    private List<String> geographicTaxonomyCodes;

    private String rea;
    private String shareCapital;
    private String businessRegisterPlace;
    private String supportEmail;
    private String supportPhone;
    private String ivassCode;
    private boolean imported;
    private AdditionalInformationsRequest additionalInformations;
}
