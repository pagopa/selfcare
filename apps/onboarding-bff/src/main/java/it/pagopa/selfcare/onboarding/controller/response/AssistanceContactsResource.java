package it.pagopa.selfcare.onboarding.controller.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class AssistanceContactsResource {

    @Schema(description = "${openapi.onboarding.institutions.model.assistance.supportEmail}")
    private String supportEmail;

    @Schema(description = "${openapi.onboarding.institutions.model.assistance.supportPhone}")
    private String supportPhone;

}
