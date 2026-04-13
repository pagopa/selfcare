package it.pagopa.selfcare.onboarding.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class MatchInfoResultResource {

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.matchResult}")
    private boolean verificationResult;

}
