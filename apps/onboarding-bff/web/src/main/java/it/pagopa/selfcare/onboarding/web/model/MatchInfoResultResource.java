package it.pagopa.selfcare.onboarding.web.model;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class MatchInfoResultResource {

    @Schema(description = "${swagger.onboarding.institutions.model.matchResult}")
    private boolean verificationResult;

}
