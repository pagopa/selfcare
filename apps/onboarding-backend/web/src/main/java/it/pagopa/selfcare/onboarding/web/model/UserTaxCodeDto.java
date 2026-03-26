package it.pagopa.selfcare.onboarding.web.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserTaxCodeDto {

    @ApiModelProperty(value = "${swagger.onboarding.user.model.fiscalCode}", required = true)
    @JsonProperty(required = true)
    @NotBlank
    private String taxCode;

}
