package it.pagopa.selfcare.onboarding.controller.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
public class CompanyUserDto {


    @Schema(description = "${openapi.onboarding.user.model.name}", required = true)
    @JsonProperty(required = true)
    @NotBlank
    private String name;

    @Schema(description = "${openapi.onboarding.user.model.surname}", required = true)
    @JsonProperty(required = true)
    @NotBlank
    private String surname;

    @Schema(description = "${openapi.onboarding.user.model.email}")
    private String email;

    @Schema(description = "${openapi.onboarding.user.model.fiscalCode}", required = true)
    @JsonProperty(required = true)
    @NotBlank
    private String taxCode;

    @Schema(description = "${openapi.onboarding.user.model.role}", required = true)
    @JsonProperty(required = true)
    private PartyRole role;

    @Schema(hidden = true)
    private String productRole;

}
