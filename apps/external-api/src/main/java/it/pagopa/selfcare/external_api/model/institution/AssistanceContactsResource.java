package it.pagopa.selfcare.external_api.model.institution;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class AssistanceContactsResource {

    @Schema(description = "${swagger.external_api.institutions.model.assistance.supportEmail}")
    private String supportEmail;

    @Schema(description = "${swagger.external_api.institutions.model.assistance.supportPhone}")
    private String supportPhone;

}
