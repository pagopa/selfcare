package it.pagopa.selfcare.onboarding.web.model;

import it.pagopa.selfcare.onboarding.connector.model.onboarding.InstitutionOnboarding;
import lombok.Data;

import java.util.List;

@Data
public class InstitutionOnboardingResource {
    private String institutionId;
    private String businessName;
    private List<InstitutionOnboarding> onboardings;
}
