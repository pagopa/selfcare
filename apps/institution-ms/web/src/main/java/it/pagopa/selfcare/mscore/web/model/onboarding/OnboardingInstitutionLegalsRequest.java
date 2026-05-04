package it.pagopa.selfcare.mscore.web.model.onboarding;

import it.pagopa.selfcare.mscore.web.model.user.Person;
import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
public class OnboardingInstitutionLegalsRequest {

    @NotEmpty(message = "productId is required")
    private String productId;

    private String productName;

    @NotEmpty(message = "at least one user is required")
    private List<Person> users;

    private String institutionExternalId;

    @NotEmpty(message = "institutionId is required")
    private String institutionId;

    @Valid
    @NotNull(message = "contract is required")
    private ContractRequest contract;

    private boolean signContract;

}
