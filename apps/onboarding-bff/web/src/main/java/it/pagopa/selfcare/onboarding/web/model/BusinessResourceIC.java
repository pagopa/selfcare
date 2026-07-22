package it.pagopa.selfcare.onboarding.web.model;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class BusinessResourceIC {

    @Schema(description = "${swagger.onboarding.institutions.businessIc.model.businessName}")
    private String businessName;

    @Schema(description = "${swagger.onboarding.institutions.businessIc.model.businessTaxId}")
    private String businessTaxId;

}
