package it.pagopa.selfcare.onboarding.controller.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class CheckManagerResponse {
    @Schema(description = "${openapi.user.check-manager.model.result}", required = true)
    @JsonProperty(required = true)
    private boolean result;

    public CheckManagerResponse(boolean result) {
        this.result = result;
    }
}
