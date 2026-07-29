package it.pagopa.selfcare.onboarding.web.model;


import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Data
public class DpoDataDto {

    @Schema(description = "${swagger.onboarding.institutions.model.pspData.dpoData.address}", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty(required = true)
    @NotBlank
    private String address;

    @Schema(description = "${swagger.onboarding.institutions.model.pspData.dpoData.pec}", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty(required = true)
    @NotBlank
    @Email
    private String pec;

    @Schema(description = "${swagger.onboarding.institutions.model.pspData.dpoData.email}", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty(required = true)
    @NotBlank
    @Email
    private String email;

}
