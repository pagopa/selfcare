package it.pagopa.selfcare.onboarding.web.model;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ManagerInfoResponse {
    @Schema(description = "${swagger.onboarding.user.model.name}", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;
    @Schema(description = "${swagger.onboarding.user.model.surname}", requiredMode = Schema.RequiredMode.REQUIRED)
    private String surname;
}
