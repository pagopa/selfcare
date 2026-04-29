package it.pagopa.selfcare.mscore.model.onboarding;

import it.pagopa.selfcare.mscore.model.institution.Institution;
import it.pagopa.selfcare.mscore.model.institution.Onboarding;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OnboardingRollback {
    private Token token;
    private Onboarding onboarding;
    private Map<String, OnboardedProduct> productMap;
    private Institution updatedInstitution;

}
