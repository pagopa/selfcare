package it.pagopa.selfcare.onboarding.controller.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class BusinessResourceIC {

    @Schema(description = "${openapi.onboarding.institutions.businessIc.model.businessName}")
    private String businessName;

    @Schema(description = "${openapi.onboarding.institutions.businessIc.model.businessTaxId}")
    private String businessTaxId;

}
