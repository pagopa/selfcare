package it.pagopa.selfcare.onboarding.model.dto.response;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "${openapi.onboarding.institutions.model.assistance}")
public class AssistanceContactsResource {

    @Schema(description = "${openapi.onboarding.institutions.model.assistance.supportEmail}")
    private String supportEmail;

    @Schema(description = "${openapi.onboarding.institutions.model.assistance.supportPhone}")
    private String supportPhone;

}
