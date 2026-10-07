package it.pagopa.selfcare.onboarding.controller.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class MatchInfoResultResource {

    @Schema(description = "${openapi.onboarding.institutions.model.matchResult}")
    private boolean verificationResult;

}
