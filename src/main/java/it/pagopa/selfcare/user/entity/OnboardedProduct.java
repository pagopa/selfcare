package it.pagopa.selfcare.user.entity;

import it.pagopa.selfcare.user.common.Env;
import it.pagopa.selfcare.user.common.PartyRole;
import it.pagopa.selfcare.user.constant.OnboardedProductState;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.time.OffsetDateTime;

import static it.pagopa.selfcare.user.common.Env.ROOT;

@Data
@FieldNameConstants(asEnum = true)
public class OnboardedProduct {

    private String productId;
    private String relationshipId;
    private String tokenId;
    private OnboardedProductState status;
    private String productRole;
    private PartyRole role;
    private Env env = ROOT;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
