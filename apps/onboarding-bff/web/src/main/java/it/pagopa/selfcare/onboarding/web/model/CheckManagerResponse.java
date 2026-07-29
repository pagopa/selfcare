package it.pagopa.selfcare.onboarding.web.model;


import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CheckManagerResponse {
    @Schema(description = "${swagger.user.check-manager.model.result}", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty(required = true)
    private boolean result;

    public CheckManagerResponse(boolean result) {
        this.result = result;
    }
}
