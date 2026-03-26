package it.pagopa.selfcare.onboarding.web.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import it.pagopa.selfcare.commons.base.security.PartyRole;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
public class CompanyUserDto {


    @ApiModelProperty(value = "${swagger.onboarding.user.model.name}", required = true)
    @JsonProperty(required = true)
    @NotBlank
    private String name;

    @ApiModelProperty(value = "${swagger.onboarding.user.model.surname}", required = true)
    @JsonProperty(required = true)
    @NotBlank
    private String surname;

    @ApiModelProperty(value = "${swagger.onboarding.user.model.email}")
    private String email;

    @ApiModelProperty(value = "${swagger.onboarding.user.model.fiscalCode}", required = true)
    @JsonProperty(required = true)
    @NotBlank
    private String taxCode;

    @ApiModelProperty(value = "${swagger.onboarding.user.model.role}", required = true)
    @JsonProperty(required = true)
    private PartyRole role;

    @ApiModelProperty(hidden = true)
    private String productRole;

}
