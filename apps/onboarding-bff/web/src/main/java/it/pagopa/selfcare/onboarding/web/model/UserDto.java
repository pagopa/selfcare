package it.pagopa.selfcare.onboarding.web.model;


import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
public class UserDto {


    @Schema(description = "${swagger.onboarding.user.model.name}", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty(required = true)
    @NotBlank
    private String name;

    @Schema(description = "${swagger.onboarding.user.model.surname}", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty(required = true)
    @NotBlank
    private String surname;

    @Schema(description = "${swagger.onboarding.user.model.fiscalCode}", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty(required = true)
    @NotBlank
    private String taxCode;

    @Schema(description = "${swagger.onboarding.user.model.role}", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty(required = true)
    private PartyRole role;

    @Schema(description = "${swagger.onboarding.user.model.email}")
    private String email;

    @Schema(hidden = true)
    private String productRole;

}
