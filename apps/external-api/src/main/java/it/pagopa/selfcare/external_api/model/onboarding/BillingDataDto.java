package it.pagopa.selfcare.external_api.model.onboarding;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
public class BillingDataDto {

    @Schema(description = "${swagger.external_api.institutions.model.name}")
    @JsonProperty(required = true)
    @NotBlank
    private String businessName;

    @Schema(description = "${swagger.external_api.institutions.model.address}")
    @JsonProperty(required = true)
    @NotBlank
    private String registeredOffice;

    @Schema(description = "${swagger.external_api.institutions.model.digitalAddress}")
    @JsonProperty(required = true)
    @NotBlank
    private String digitalAddress;

    @Schema(description = "${swagger.external_api.institutions.model.zipCode}")
    private String zipCode;

    @Schema(description = "${swagger.external_api.institutions.model.taxCode}")
    private String taxCode;

    @Schema(description = "${swagger.external_api.institutions.model.taxCodeInvoicing}")
    private String taxCodeInvoicing;

    @Schema(description = "${swagger.external_api.institutions.model.vatNumber}")
    private String vatNumber;

    @Schema(description = "${swagger.external_api.institutions.model.recipientCode}")
    private String recipientCode;

    @Schema(description = "${swagger.external_api.institutions.model.publicServices}", example = "false")
    private Boolean publicServices;

}
