package it.pagopa.selfcare.onboarding.controller.response;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import lombok.Data;

@Data
public class MatchInfoResultResource {

    @Schema(description = "${openapi.onboarding.institutions.model.matchResult}")
    private boolean verificationResult;

}
