package it.pagopa.selfcare.onboarding.controller.response;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import lombok.Data;

@Data
public class AssistanceContactsResource {

    @Schema(description = "${openapi.onboarding.institutions.model.assistance.supportEmail}")
    private String supportEmail;

    @Schema(description = "${openapi.onboarding.institutions.model.assistance.supportPhone}")
    private String supportPhone;

}
