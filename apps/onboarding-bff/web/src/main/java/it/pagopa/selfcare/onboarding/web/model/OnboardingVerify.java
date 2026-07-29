package it.pagopa.selfcare.onboarding.web.model;


import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OnboardingVerify {


    @Schema(description = "${swagger.onboarding.model.status}", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty(required = true)
    private String status;

    @Schema(description = "${swagger.onboarding.product.model.id}")
    private String productId;

    @Schema(description = "${swagger.onboarding.model.expiringDate}")
    private LocalDateTime expiringDate;

}
