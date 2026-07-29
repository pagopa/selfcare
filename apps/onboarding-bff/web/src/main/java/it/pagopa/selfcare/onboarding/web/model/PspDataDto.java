package it.pagopa.selfcare.onboarding.web.model;


import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class PspDataDto {

    @Schema(description = "${swagger.onboarding.institutions.model.pspData.businessRegisterNumber}", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty(required = true)
    @NotBlank
    private String businessRegisterNumber;

    @Schema(description = "${swagger.onboarding.institutions.model.pspData.legalRegisterName}", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty(required = true)
    @NotBlank
    private String legalRegisterName;

    @Schema(description = "${swagger.onboarding.institutions.model.pspData.legalRegisterNumber}", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty(required = true)
    @NotBlank
    private String legalRegisterNumber;

    @Schema(description = "${swagger.onboarding.institutions.model.pspData.abiCode}", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty(required = true)
    @NotBlank
    private String abiCode;

    @Schema(description = "${swagger.onboarding.institutions.model.pspData.vatNumberGroup}", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty(required = true)
    @NotNull
    private Boolean vatNumberGroup;

    @Schema(description = "${swagger.onboarding.institutions.model.pspData.dpoData}", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    @Valid
    private DpoDataDto dpoData;

}
