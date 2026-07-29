package it.pagopa.selfcare.onboarding.web.model;


import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
public class UserDataValidationDto {

    @Schema(description = "${swagger.onboarding.user.model.fiscalCode}", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty(required = true)
    @NotBlank
    private String taxCode;

    @Schema(description = "${swagger.onboarding.user.model.name}")
    private String name;

    @Schema(description = "${swagger.onboarding.user.model.surname}")
    private String surname;

}
