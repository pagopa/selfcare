package it.pagopa.selfcare.onboarding.web.model;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class VerifyManagerResponse {
    @Schema(description = "${swagger.onboarding.institutions.model.origin}")
    private String origin;

    @Schema(description = "${swagger.onboarding.institutions.model.name}")
    private String companyName;
}
