package it.pagopa.selfcare.mscore.web.model.institution;

import jakarta.validation.Valid;
import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
@ToString
public class InstitutionPut {

    List<String> geographicTaxonomyCodes;
    private String digitalAddress;
    private String description;
    private String parentDescription;
    private String address;
    private String zipCode;
    private String rea;
    private String shareCapital;
    private String businessRegisterPlace;
    private String supportEmail;
    @Valid
    private List<OnboardingPut> onboardings;
}
