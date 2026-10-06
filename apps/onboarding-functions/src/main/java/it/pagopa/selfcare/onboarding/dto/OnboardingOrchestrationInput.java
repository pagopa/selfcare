package it.pagopa.selfcare.onboarding.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingOrchestrationInput {

  private String onboardingId;
  private String tenantId;
}
