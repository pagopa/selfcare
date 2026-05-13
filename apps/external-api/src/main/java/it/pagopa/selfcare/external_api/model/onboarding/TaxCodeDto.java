package it.pagopa.selfcare.external_api.model.onboarding;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class TaxCodeDto {

    @Schema(description = "${swagger.external_api.institutions.model.aggregates.taxCode}")
    @NotEmpty
    @Valid
    String taxCode;
}
