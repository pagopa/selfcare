package it.pagopa.selfcare.onboarding.controller.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ReasonForRejectDto {

    @Schema(description = "${openapi.onboarding.institution.model.reason}")
    private String reason;
}
