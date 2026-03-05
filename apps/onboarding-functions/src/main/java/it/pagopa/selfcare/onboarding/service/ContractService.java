package it.pagopa.selfcare.onboarding.service;

import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.OnboardingAttachment;
import it.pagopa.selfcare.onboarding.entity.OnboardingWorkflow;

import java.io.File;
import java.util.List;
import java.util.Optional;

import org.openapi.quarkus.user_registry_json.model.UserResource;

public interface ContractService {
  File createContractPDF(
    String contractTemplatePath,
    Onboarding onboarding,
    UserResource manager,
    List<UserResource> users,
    String productName,
    String pdfFormatFilename);

  File createAttachmentPDF(
    String templatePath, Onboarding onboarding, String productName, String pdfFormatFilename, UserResource userResource);

  File loadContractPDF(String contractTemplatePath, String onboardingId, String productName);

  File retrieveContractNotSigned(OnboardingWorkflow onboardingWorkflow, String productName);

  File retrieveAttachment(OnboardingAttachment onboardingAttachment, String productName);

  Optional<File> getLogoFile();

  void uploadAggregatesCsv(OnboardingWorkflow onboardingWorkflow);

  String deleteContract(String filename, boolean absolutePath);
}
