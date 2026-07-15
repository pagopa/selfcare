package it.pagopa.selfcare.onboarding.connector;

import io.github.resilience4j.retry.annotation.Retry;
import it.pagopa.selfcare.document.generated.openapi.v1.dto.DocumentBuilderRequest;
import it.pagopa.selfcare.document.generated.openapi.v1.dto.DocumentType;

import it.pagopa.selfcare.document.generated.openapi.v1.dto.UserAttachmentRequest;
import it.pagopa.selfcare.onboarding.connector.api.DocumentMsConnector;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.AvailableDocuments;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.OnboardingData;
import it.pagopa.selfcare.onboarding.connector.rest.client.MsDocumentApiClient;
import it.pagopa.selfcare.onboarding.connector.rest.client.MsDocumentContentApiClient;
import it.pagopa.selfcare.onboarding.connector.rest.mapper.DocumentMapper;
import it.pagopa.selfcare.product.entity.AttachmentTemplate;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


@Service
@Slf4j
public class DocumentMsConnectorImpl implements DocumentMsConnector {

  private final MsDocumentContentApiClient msDocumentContentApiClient;
  private final MsDocumentApiClient msDocumentApiClient;
  private final DocumentMapper documentMapper;

  public DocumentMsConnectorImpl(
      MsDocumentContentApiClient msDocumentContentApiClient,
      MsDocumentApiClient msDocumentApiClient,
      DocumentMapper documentMapper) {
    this.msDocumentContentApiClient = msDocumentContentApiClient;
    this.msDocumentApiClient = msDocumentApiClient;
    this.documentMapper = documentMapper;
  }

  @Override
  @Retry(name = "retryTimeout")
  public Resource getContract(String onboardingId) {
    return msDocumentContentApiClient._getContract(onboardingId).getBody();
  }

  @Override
  @Retry(name = "retryTimeout")
  public Resource getTemplateAttachment(
      OnboardingData onboarding, String filename, String templatePath) {
    return msDocumentContentApiClient
        ._getTemplateAttachment(
            onboarding.getId(),
            onboarding.getInstitutionUpdate().getDescription(),
            filename,
            onboarding.getProductId(),
            templatePath)
        .getBody();
  }

  @Override
  @Retry(name = "retryTimeout")
  public Resource getAttachment(String onboardingId, String filename) {
    return msDocumentContentApiClient._getAttachment(onboardingId, filename).getBody();
  }

  @Override
  @Retry(name = "retryTimeout")
  public AvailableDocuments getAvailableDocuments(String onboardingId) {
    log.info("getAvailableDocuments for onboardingId: {}", Encode.forJava(onboardingId));
    return documentMapper.toAvailableDocuments(
            msDocumentApiClient._getAvailableDocuments(onboardingId).getBody());
  }

  @Override
  public HttpStatusCode headAttachment(String onboardingId, String filename) {
    log.info("headAttachment for onboardingId: {}, filename: {}", Encode.forJava(onboardingId), Encode.forJava(filename));
    ResponseEntity<Void> responseEntity = msDocumentApiClient._headAttachment(onboardingId, filename);
    log.info("headAttachment response status code: {}", responseEntity.getStatusCode());
    return HttpStatus.resolve(responseEntity.getStatusCode().value());
  }

  @Override
  public void uploadAttachment(String onboardingId, MultipartFile attachment, String attachmentName,
                               String productId, AttachmentTemplate template) {
    DocumentBuilderRequest request = new DocumentBuilderRequest();
    request.setAttachmentName(attachmentName);
    request.setProductId(productId);
    request.setOnboardingId(onboardingId);
    request.setTemplatePath(template.getTemplatePath());
    request.setTemplateVersion(template.getTemplateVersion());
    request.setDocumentType(DocumentType.ATTACHMENT);
    msDocumentContentApiClient._uploadAttachment(attachment, request);
  }

  @Override
  public void uploadUserAttachment(String onboardingId, MultipartFile attachment, String productId,
                                   String attachmentId, String attachmentDescription, String attachmentName,
                                   Integer maxDocumentsRequired) {
    UserAttachmentRequest request = new UserAttachmentRequest();
    request.setOnboardingId(onboardingId);
    request.setProductId(productId);
    request.setAttachmentId(attachmentId);
    request.setAttachmentDescription(attachmentDescription);
    request.setAttachmentName(attachmentName);
    request.setMaxDocumentsRequired(maxDocumentsRequired);
    msDocumentContentApiClient._uploadUserAttachment(attachment, request);
  }


  @Override
  @Retry(name = "retryTimeout")
  public Resource getAggregatesCsv(String onboardingId, String productId) {
    return msDocumentContentApiClient._getAggregatesCsv(onboardingId, productId).getBody();
  }
}
