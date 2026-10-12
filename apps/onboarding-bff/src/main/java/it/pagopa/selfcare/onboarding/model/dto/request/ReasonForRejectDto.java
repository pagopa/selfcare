package it.pagopa.selfcare.onboarding.model.dto.request;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import lombok.Data;

@Data
public class ReasonForRejectDto {

    @Schema(description = "${openapi.onboarding.institution.model.reason}")
    private String reason;
}
