package it.pagopa.selfcare.onboarding.controller.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class PspDataDto {

    @Schema(description = "${openapi.onboarding.institutions.model.pspData.businessRegisterNumber}", required = true)
    @JsonProperty(required = true)
    @NotBlank
    private String businessRegisterNumber;

    @Schema(description = "${openapi.onboarding.institutions.model.pspData.legalRegisterName}", required = true)
    @JsonProperty(required = true)
    @NotBlank
    private String legalRegisterName;

    @Schema(description = "${openapi.onboarding.institutions.model.pspData.legalRegisterNumber}", required = true)
    @JsonProperty(required = true)
    @NotBlank
    private String legalRegisterNumber;

    @Schema(description = "${openapi.onboarding.institutions.model.pspData.abiCode}", required = true)
    @JsonProperty(required = true)
    @NotBlank
    private String abiCode;

    @Schema(description = "${openapi.onboarding.institutions.model.pspData.vatNumberGroup}", required = true)
    @JsonProperty(required = true)
    @NotNull
    private Boolean vatNumberGroup;

    @Schema(description = "${openapi.onboarding.institutions.model.pspData.dpoData}", required = true)
    @NotNull
    @Valid
    private DpoDataDto dpoData;

}
