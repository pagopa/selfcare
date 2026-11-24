package it.pagopa.selfcare.onboarding.controller.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

import static it.pagopa.selfcare.onboarding.common.ProductId.PROD_RTP;

@EqualsAndHashCode(callSuper = true)
@Data
public class OnboardingPspRequest extends OnboardingBaseRequest {

    @NotNull(message = "institutionData is required")
    @Valid
    private InstitutionPspRequest institution;
    private BillingRequest billing;
    @Schema(description = "Required and non-empty when productId is prod-rtp.")
    private List<RtpServiceTypeRequest> rtpServiceTypes;

    @AssertTrue(message = "rtpServiceTypes is required and must not be empty for prod-rtp")
    @JsonIgnore
    @Schema(hidden = true)
    public boolean isRtpServiceTypesValid() {
        return !PROD_RTP.getValue().equals(getProductId())
                || (rtpServiceTypes != null && !rtpServiceTypes.isEmpty());
    }

}
