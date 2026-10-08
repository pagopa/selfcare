package it.pagopa.selfcare.onboarding.model.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Data
@Schema(description = "${openapi.onboarding.institutions.model.dpoData}")
public class DpoDataDto {

    @Schema(description = "${openapi.onboarding.institutions.model.pspData.dpoData.address}", required = true)
    @JsonProperty(required = true)
    @NotBlank
    private String address;

    @Schema(description = "${openapi.onboarding.institutions.model.pspData.dpoData.pec}", required = true)
    @JsonProperty(required = true)
    @NotBlank
    @Email
    private String pec;

    @Schema(description = "${openapi.onboarding.institutions.model.pspData.dpoData.email}", required = true)
    @JsonProperty(required = true)
    @NotBlank
    @Email
    private String email;

}
