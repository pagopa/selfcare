package it.pagopa.selfcare.onboarding.controller.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserTaxCodeDto {

    @Schema(description = "${openapi.onboarding.user.model.fiscalCode}", required = true)
    @JsonProperty(required = true)
    @NotBlank
    private String taxCode;

}
