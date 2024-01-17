package it.pagopa.selfcare.user.controller.response;

import it.pagopa.selfcare.user.common.Env;
import it.pagopa.selfcare.user.common.PartyRole;
import it.pagopa.selfcare.user.constant.OnboardedProductState;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class OnboardedProductResponse {

    private String productId;
    private String relationshipId;
    private String tokenId;
    private OnboardedProductState status;
    private String productRole;
    private PartyRole role;
    private Env env;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

}
