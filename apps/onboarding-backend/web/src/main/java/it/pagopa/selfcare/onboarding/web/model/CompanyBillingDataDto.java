package it.pagopa.selfcare.onboarding.web.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class CompanyBillingDataDto {

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.name}", required = true)
    @JsonProperty(required = true)
    private String businessName;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.taxCode}", required = true)
    @JsonProperty(required = true)
    @NotBlank
    private String taxCode;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.certified}", required = true)
    @JsonProperty(required = true)
    @NotNull
    private boolean certified;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.digitalAddress}", required = true)
    @JsonProperty(required = true)
    @NotBlank
    private String digitalAddress;

}
