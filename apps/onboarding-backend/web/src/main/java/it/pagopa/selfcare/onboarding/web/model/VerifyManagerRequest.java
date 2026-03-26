package it.pagopa.selfcare.onboarding.web.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
public class VerifyManagerRequest {
    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.taxCode}")
    @JsonProperty(required = true)
    @NotBlank
    private String companyTaxCode;
}
