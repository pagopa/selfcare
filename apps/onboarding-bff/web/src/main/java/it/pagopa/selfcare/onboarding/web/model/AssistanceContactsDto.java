package it.pagopa.selfcare.onboarding.web.model;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.Email;

@Data
public class AssistanceContactsDto {

    @Schema(description = "${swagger.onboarding.institutions.model.assistance.supportEmail}")
    @Email
    private String supportEmail;

    @Schema(description = "${swagger.onboarding.institutions.model.assistance.supportPhone}")
    private String supportPhone;

}
