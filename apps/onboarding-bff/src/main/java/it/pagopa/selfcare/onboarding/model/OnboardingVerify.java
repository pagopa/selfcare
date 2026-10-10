package it.pagopa.selfcare.onboarding.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OnboardingVerify {


    @Schema(description = "${openapi.onboarding.model.status}", required = true)
    @JsonProperty(required = true)
    private String status;

    @Schema(description = "${openapi.onboarding.product.model.id}")
    private String productId;

    @Schema(description = "${openapi.onboarding.model.expiringDate}")
    private LocalDateTime expiringDate;

}
