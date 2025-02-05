package it.pagopa.selfcare.user.controller.response;

import it.pagopa.selfcare.onboarding.common.Env;
import it.pagopa.selfcare.user.model.constants.OnboardedProductState;
import lombok.Data;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.OffsetDateTime;

@Data
public class OnboardedProductResponse {

    private String productId;
    private String tokenId;
    private OnboardedProductState status;
    private String productRole;
    @Schema(description = "Available values: MANAGER, DELEGATE, SUB_DELEGATE, OPERATOR, ADMIN_EA, ADMIN_EA_IO")
    private String role;
    private Env env;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

}
