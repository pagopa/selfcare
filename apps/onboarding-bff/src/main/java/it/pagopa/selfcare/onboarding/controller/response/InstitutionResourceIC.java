package it.pagopa.selfcare.onboarding.controller.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class InstitutionResourceIC {

    @Schema(description = "${openapi.onboarding.institutions.model.legalTaxId}")
    private String legalTaxId;

    @Schema(description = "${openapi.onboarding.institutions.model.requestDateTime}")
    private String requestDateTime;

    @Schema(description = "${openapi.onboarding.institutions.model.businesses}")
    private List<BusinessResourceIC> businesses;

}
