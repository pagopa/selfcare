package it.pagopa.selfcare.onboarding.steps;

import it.pagopa.selfcare.cucumber.utils.SharedStepData;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UserSteps {

  private final SharedStepData sharedStepData;

  public UserSteps(SharedStepData sharedStepData) {
    this.sharedStepData = sharedStepData;
  }
}
