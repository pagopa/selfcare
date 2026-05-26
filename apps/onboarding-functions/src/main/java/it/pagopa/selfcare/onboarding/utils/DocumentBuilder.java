package it.pagopa.selfcare.onboarding.utils;

import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.OnboardingWorkflow;
import it.pagopa.selfcare.onboarding.mapper.AttachmentPdfRequestMapper;
import it.pagopa.selfcare.onboarding.mapper.ContractPdfRequestMapper;
import it.pagopa.selfcare.onboarding.mapper.DocumentBuilderRequestMapper;
import it.pagopa.selfcare.product.entity.AttachmentTemplate;
import it.pagopa.selfcare.product.entity.Product;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import org.openapi.quarkus.document_json.model.AttachmentPdfRequest;
import org.openapi.quarkus.document_json.model.ContractPdfRequest;
import org.openapi.quarkus.document_json.model.DocumentBuilderRequest;
import org.openapi.quarkus.document_json.model.DocumentType;
import org.openapi.quarkus.user_registry_json.model.UserResource;

@ApplicationScoped
public class DocumentBuilder {

  private final ContractPdfRequestMapper contractPdfRequestMapper;
  private final AttachmentPdfRequestMapper attachmentPdfRequestMapper;
  private final DocumentBuilderRequestMapper documentBuilderRequestMapper;

  @Inject
  public DocumentBuilder(
    ContractPdfRequestMapper contractPdfRequestMapper,
    AttachmentPdfRequestMapper attachmentPdfRequestMapper,
    DocumentBuilderRequestMapper documentBuilderRequestMapper) {
    this.contractPdfRequestMapper = contractPdfRequestMapper;
    this.attachmentPdfRequestMapper = attachmentPdfRequestMapper;
    this.documentBuilderRequestMapper = documentBuilderRequestMapper;
  }

  public ContractPdfRequest toContractPdfRequest(
    Onboarding onboarding,
    UserResource manager,
    List<UserResource> delegates,
    Product product,
    String contractTemplatePath,
    String rejectOnboardingUrl) {
    return contractPdfRequestMapper.toRequest(
      onboarding,
      manager,
      delegates,
      product,
      contractTemplatePath,
      rejectOnboardingUrl);
  }

  public AttachmentPdfRequest toAttachmentPdfRequest(
    Onboarding onboarding,
    AttachmentTemplate attachmentTemplate,
    Product product,
    UserResource manager) {
    return attachmentPdfRequestMapper.toRequest(onboarding, attachmentTemplate, product, manager);
  }

  public DocumentBuilderRequest toContractDocumentBuilderRequest(
    Onboarding onboarding,
    Product product,
    OnboardingWorkflow onboardingWorkflow) {
    return documentBuilderRequestMapper.toRequest(onboarding, product, onboardingWorkflow);
  }

  public DocumentBuilderRequest toAttachmentDocumentBuilderRequest(
    Onboarding onboarding,
    Product product,
    AttachmentTemplate attachmentTemplate,
    DocumentType documentType) {
    return documentBuilderRequestMapper.toRequest(onboarding, product, attachmentTemplate, documentType);
  }
}
