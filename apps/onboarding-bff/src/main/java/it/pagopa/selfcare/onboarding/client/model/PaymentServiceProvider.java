package it.pagopa.selfcare.onboarding.client.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "${openapi.onboarding.institutions.model.paymentServiceProvider}")
public class PaymentServiceProvider extends BusinessData {

    private String abiCode;
    private Boolean vatNumberGroup;

}
