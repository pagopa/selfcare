package it.pagopa.selfcare.onboarding.model;

import it.pagopa.selfcare.onboarding.client.model.onboarding.InstitutionOnboarding;
import lombok.Data;

import java.util.List;

@Data
public class InstitutionOnboardingResource {
    private String institutionId;
    private String businessName;
    private List<InstitutionOnboarding> onboardings;
}
