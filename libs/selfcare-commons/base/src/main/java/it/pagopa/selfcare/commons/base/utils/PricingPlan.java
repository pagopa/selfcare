package it.pagopa.selfcare.commons.base.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PricingPlan {

    FA("FAST"),
    BASE("BASE"),
    PREMIUM("PREMIUM");

    private final String value;

}
