package it.pagopa.selfcare.onboarding.model.dto.response;

import it.pagopa.selfcare.onboarding.client.model.InstitutionOnboarding;
import lombok.Data;

import java.util.List;

@Data
public class InstitutionOnboardingResource {
    private String institutionId;
    private String businessName;
    private List<InstitutionOnboarding> onboardings;
}
