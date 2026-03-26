package it.pagopa.selfcare.onboarding.service;

import it.pagopa.selfcare.onboarding.entity.OnboardingWorkflow;

import java.io.File;
import java.util.Optional;

public interface ContractService {

  Optional<File> getLogoFile();

  void uploadAggregatesCsv(OnboardingWorkflow onboardingWorkflow);

}
