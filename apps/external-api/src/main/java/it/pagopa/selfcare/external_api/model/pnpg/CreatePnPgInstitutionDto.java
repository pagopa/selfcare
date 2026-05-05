package it.pagopa.selfcare.external_api.model.pnpg;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
public class CreatePnPgInstitutionDto {
    @Schema(description = "${swagger.external_api.institutions.model.externalId}")
    @JsonProperty(required = true)
    @NotBlank
    private String externalId;
    @Schema(description = "${swagger.external_api.institutions.model.name}")
    private String description;
}
