package it.pagopa.selfcare.onboarding.controller.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class BillingRequest {
    private String vatNumber;
    private String recipientCode;
    private boolean publicServices;
}
