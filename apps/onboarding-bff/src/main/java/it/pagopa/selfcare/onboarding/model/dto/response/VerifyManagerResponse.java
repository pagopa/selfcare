package it.pagopa.selfcare.onboarding.model.dto.response;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import lombok.Data;

@Data
public class VerifyManagerResponse {
    @Schema(description = "${openapi.onboarding.institutions.model.origin}")
    private String origin;

    @Schema(description = "${openapi.onboarding.institutions.model.name}")
    private String companyName;
}
