package it.pagopa.selfcare.onboarding.service;

import it.pagopa.selfcare.onboarding.entity.OnboardingWorkflow;
import org.openapi.quarkus.document_json.api.DocumentContentControllerApi;

import java.io.File;
import java.util.Optional;

public interface ContractService {

  Optional<File> getLogoFile();

  DocumentContentControllerApi.UploadAggregatesCsvMultipartForm requestUploadAggregatesCsv(OnboardingWorkflow onboardingWorkflow);

}
