package it.pagopa.selfcare.onboarding.web.model;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ReasonForRejectDto {

    @Schema(description = "${swagger.onboarding.institution.model.reason}")
    private String reason;
}
