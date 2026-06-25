package it.pagopa.selfcare.external_api.model.institution;

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
    private PspDataResource paymentServiceProvider;
    private DpoDataResource dataProtectionOfficer;
    private String businessRegisterPlace;
    private List<String> geographicTaxonomies;
    private boolean imported;
    private String rea;
    private String shareCapital;
    private String supportEmail;
    private String supportPhone;
}

