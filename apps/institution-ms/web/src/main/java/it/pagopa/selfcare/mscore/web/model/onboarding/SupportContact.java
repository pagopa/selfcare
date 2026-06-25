package it.pagopa.selfcare.mscore.web.model.onboarding;

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
