package it.pagopa.selfcare.onboarding.controller.response;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import lombok.Data;

@Data
public class ManagerInfoResponse {
    @Schema(description = "${openapi.onboarding.user.model.name}", required = true)
    private String name;
    @Schema(description = "${openapi.onboarding.user.model.surname}", required = true)
    private String surname;
}
