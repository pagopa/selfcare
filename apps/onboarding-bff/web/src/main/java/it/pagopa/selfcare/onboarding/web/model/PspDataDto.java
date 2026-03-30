package it.pagopa.selfcare.onboarding.web.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class PspDataDto {

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.pspData.businessRegisterNumber}", required = true)
    @JsonProperty(required = true)
    @NotBlank
    private String businessRegisterNumber;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.pspData.legalRegisterName}", required = true)
    @JsonProperty(required = true)
    @NotBlank
    private String legalRegisterName;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.pspData.legalRegisterNumber}", required = true)
    @JsonProperty(required = true)
    @NotBlank
    private String legalRegisterNumber;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.pspData.abiCode}", required = true)
    @JsonProperty(required = true)
    @NotBlank
    private String abiCode;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.pspData.vatNumberGroup}", required = true)
    @JsonProperty(required = true)
    @NotNull
    private Boolean vatNumberGroup;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.pspData.dpoData}", required = true)
    @NotNull
    @Valid
    private DpoDataDto dpoData;

}
