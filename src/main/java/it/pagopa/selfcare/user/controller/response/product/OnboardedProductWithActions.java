package it.pagopa.selfcare.user.controller.response.product;

import it.pagopa.selfcare.onboarding.common.Env;
import it.pagopa.selfcare.user.model.constants.OnboardedProductState;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

import static it.pagopa.selfcare.onboarding.common.Env.ROOT;


@Data
@FieldNameConstants(asEnum = true)
public class OnboardedProductWithActions {

    private String productId;
    private String tokenId;
    private OnboardedProductState status;
    private String productRole;
    @Schema(description = "Available values: MANAGER, DELEGATE, SUB_DELEGATE, OPERATOR, ADMIN_EA, ADMIN_EA_IO")
    private String role;
    private Env env = ROOT;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private String delegationId;
    private List<String> userProductActions;
}
