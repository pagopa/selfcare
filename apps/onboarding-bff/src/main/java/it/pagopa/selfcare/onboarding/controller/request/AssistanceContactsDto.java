package it.pagopa.selfcare.onboarding.controller.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.Email;

@Data
public class AssistanceContactsDto {

    @Schema(description = "${openapi.onboarding.institutions.model.assistance.supportEmail}")
    @Email
    private String supportEmail;

    @Schema(description = "${openapi.onboarding.institutions.model.assistance.supportPhone}")
    private String supportPhone;

}
