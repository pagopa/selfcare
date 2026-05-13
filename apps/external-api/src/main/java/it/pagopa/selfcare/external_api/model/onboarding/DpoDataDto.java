package it.pagopa.selfcare.external_api.model.onboarding;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Data
public class DpoDataDto {

    @Schema(description = "${swagger.external_api.institutions.model.pspData.dpoData.address}")
    @JsonProperty(required = true)
    @NotBlank
    private String address;

    @Schema(description = "${swagger.external_api.institutions.model.pspData.dpoData.pec}", format = "email", example = "email@example.com")
    @JsonProperty(required = true)
    @NotBlank
    @Email
    private String pec;

    @Schema(description = "${swagger.external_api.institutions.model.pspData.dpoData.email}", format = "email", example = "email@example.com")
    @JsonProperty(required = true)
    @NotBlank
    @Email
    private String email;

}
