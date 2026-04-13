package it.pagopa.selfcare.onboarding.client.model.onboarding;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PaymentServiceProvider extends BusinessData {

    private String abiCode;
    private Boolean vatNumberGroup;

}
