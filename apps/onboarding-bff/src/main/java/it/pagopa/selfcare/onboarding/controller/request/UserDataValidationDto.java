package it.pagopa.selfcare.onboarding.controller.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
public class UserDataValidationDto {

    @Schema(description = "${openapi.onboarding.user.model.fiscalCode}", required = true)
    @JsonProperty(required = true)
    @NotBlank
    private String taxCode;

    @Schema(description = "${openapi.onboarding.user.model.name}")
    private String name;

    @Schema(description = "${openapi.onboarding.user.model.surname}")
    private String surname;

}
