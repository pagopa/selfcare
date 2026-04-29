package it.pagopa.selfcare.mscore.web.model.onboarding;

import it.pagopa.selfcare.mscore.web.model.user.Person;
import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class OnboardingInstitutionUsersRequest {

    @NotEmpty(message = "productId is required")
    private String productId;

    @NotEmpty(message = "at least one user is required")
    private List<Person> users;

    @NotEmpty(message = "InstitutionId is required")
    private String institutionTaxCode;

    private String institutionSubunitCode;
    private Boolean sendCreateUserNotificationEmail = Boolean.TRUE;

}
