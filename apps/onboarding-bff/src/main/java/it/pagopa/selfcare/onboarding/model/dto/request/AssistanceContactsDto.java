package it.pagopa.selfcare.onboarding.model.dto.request;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.Email;

@Data
@Schema(description = "${openapi.onboarding.institutions.model.assistance}")
public class AssistanceContactsDto {

    @Schema(description = "${openapi.onboarding.institutions.model.assistance.supportEmail}")
    @Email
    private String supportEmail;

    @Schema(description = "${openapi.onboarding.institutions.model.assistance.supportPhone}")
    private String supportPhone;

}
