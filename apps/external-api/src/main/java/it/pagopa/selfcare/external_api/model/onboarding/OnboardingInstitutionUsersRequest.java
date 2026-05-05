package it.pagopa.selfcare.external_api.model.onboarding;

import io.swagger.v3.oas.annotations.media.Schema;
import it.pagopa.selfcare.external_api.model.user.Person;
import lombok.Data;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Optional;

@Data
public class OnboardingInstitutionUsersRequest {

    @Schema(description = "Product to add roles to")
    @NotEmpty(message = "productId is required")
    private String productId;

    @Schema(description = "List of users to add")
    @NotEmpty(message = "at least one user is required")
    private List<Person> users;

    @Schema(description = "The institution ID where users will be added. This takes precedence over the tax code")
    private String institutionId;

    @Schema(description = "Add users to the institution via tax code. Can be used in combination with institutionSubunitCode")
    private String institutionTaxCode;

    @Schema(description = "Add users to the institution via subunit code. Can be used in combination with institutionTaxCode")
    private String institutionSubunitCode;

    @Schema(description = "Send an email notification to the user. By default it's set to true", example = "false")
    private Boolean sendCreateUserNotificationEmail = Boolean.TRUE;

    @Schema(description = "Enable automatic management of group assignment", example = "false")
    private Boolean toAddOnAggregates;

    @Schema(hidden = true)
    @AssertTrue(message = "at least one of institutionId or institutionTaxCode must be present")
    public boolean isIdOrTaxcodeNotEmpty() {
        final boolean isIdNotEmpty = Optional.ofNullable(institutionId).map(id -> !id.isEmpty()).orElse(false);
        final boolean isTaxcodeNotEmpty = Optional.ofNullable(institutionTaxCode).map(taxcode -> !taxcode.isEmpty()).orElse(false);
        return isIdNotEmpty || isTaxcodeNotEmpty;
    }

}
