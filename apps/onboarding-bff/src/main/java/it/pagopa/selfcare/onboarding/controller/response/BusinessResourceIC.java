package it.pagopa.selfcare.onboarding.controller.response;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "${openapi.onboarding.institutions.model.businesses}")
public class BusinessResourceIC {

    @Schema(description = "${openapi.onboarding.institutions.businessIc.model.businessName}")
    private String businessName;

    @Schema(description = "${openapi.onboarding.institutions.businessIc.model.businessTaxId}")
    private String businessTaxId;

}
