package it.pagopa.selfcare.onboarding.controller.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SupportContact {
    private String supportEmail;
    private String supportPhone;
}
