package it.pagopa.selfcare.onboarding.model.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
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
