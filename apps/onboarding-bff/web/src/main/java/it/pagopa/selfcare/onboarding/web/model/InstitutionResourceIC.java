package it.pagopa.selfcare.onboarding.web.model;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class InstitutionResourceIC {

    @Schema(description = "${swagger.onboarding.institutions.model.legalTaxId}")
    private String legalTaxId;

    @Schema(description = "${swagger.onboarding.institutions.model.requestDateTime}")
    private String requestDateTime;

    @Schema(description = "${swagger.onboarding.institutions.model.businesses}")
    private List<BusinessResourceIC> businesses;

}
