package it.pagopa.selfcare.onboarding.web.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OnboardingVerify {


    @ApiModelProperty(value = "${swagger.onboarding.model.status}", required = true)
    @JsonProperty(required = true)
    private String status;

    @ApiModelProperty(value = "${swagger.onboarding.product.model.id}")
    private String productId;

    @ApiModelProperty(value = "${swagger.onboarding.model.expiringDate}")
    private LocalDateTime expiringDate;

}
