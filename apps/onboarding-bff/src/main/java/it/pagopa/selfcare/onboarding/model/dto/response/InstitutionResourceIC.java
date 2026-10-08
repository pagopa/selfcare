package it.pagopa.selfcare.onboarding.model.dto.response;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
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
