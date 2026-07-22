package it.pagopa.selfcare.onboarding.web.model;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class AssistanceContactsResource {

    @Schema(description = "${swagger.onboarding.institutions.model.assistance.supportEmail}")
    private String supportEmail;

    @Schema(description = "${swagger.onboarding.institutions.model.assistance.supportPhone}")
    private String supportPhone;

}
