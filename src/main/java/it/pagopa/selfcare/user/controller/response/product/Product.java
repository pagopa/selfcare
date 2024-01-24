package it.pagopa.selfcare.user.controller.response.product;

import it.pagopa.selfcare.onboarding.common.Env;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.user.constant.OnboardedProductState;
import lombok.Data;

import java.time.LocalDateTime;

import static it.pagopa.selfcare.onboarding.common.Env.ROOT;

@Data
public class Product {

    private String productId;
    private String tokenId;
    private OnboardedProductState status;
    private String contract;
    private String productRole;
    private PartyRole role;
    private Env env = ROOT;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}