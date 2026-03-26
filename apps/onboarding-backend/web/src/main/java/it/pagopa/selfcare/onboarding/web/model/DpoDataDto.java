package it.pagopa.selfcare.onboarding.web.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Data
public class DpoDataDto {

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.pspData.dpoData.address}", required = true)
    @JsonProperty(required = true)
    @NotBlank
    private String address;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.pspData.dpoData.pec}", required = true)
    @JsonProperty(required = true)
    @NotBlank
    @Email
    private String pec;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.pspData.dpoData.email}", required = true)
    @JsonProperty(required = true)
    @NotBlank
    @Email
    private String email;

}
