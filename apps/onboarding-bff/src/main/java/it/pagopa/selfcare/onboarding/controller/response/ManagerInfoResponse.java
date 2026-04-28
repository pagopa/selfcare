package it.pagopa.selfcare.onboarding.controller.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ManagerInfoResponse {
    @Schema(description = "${openapi.onboarding.user.model.name}", required = true)
    private String name;
    @Schema(description = "${openapi.onboarding.user.model.surname}", required = true)
    private String surname;
}
