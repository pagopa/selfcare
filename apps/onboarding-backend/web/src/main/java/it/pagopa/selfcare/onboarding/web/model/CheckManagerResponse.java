package it.pagopa.selfcare.onboarding.web.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class CheckManagerResponse {
    @ApiModelProperty(value = "${swagger.user.check-manager.model.result}", required = true)
    @JsonProperty(required = true)
    private boolean result;

    public CheckManagerResponse(boolean result) {
        this.result = result;
    }
}
