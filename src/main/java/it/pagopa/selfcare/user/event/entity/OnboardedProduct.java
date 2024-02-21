package it.pagopa.selfcare.user.event.entity;

import it.pagopa.selfcare.onboarding.common.Env;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.user.event.constant.OnboardedProductState;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.time.LocalDateTime;

import static it.pagopa.selfcare.onboarding.common.Env.ROOT;


@Data
@FieldNameConstants(asEnum = true)
public class OnboardedProduct {

    private String productId;
    private String tokenId;
    private OnboardedProductState status;
    private String productRole;
    private PartyRole role;
    private Env env = ROOT;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
