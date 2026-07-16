package it.pagopa.selfcare.onboarding.service;

import it.pagopa.selfcare.onboarding.client.model.BinaryData;
import it.pagopa.selfcare.onboarding.client.model.AvailableDocuments;
import it.pagopa.selfcare.onboarding.client.model.UploadedFile;
import it.pagopa.selfcare.onboarding.client.util.FilePayloadUtils;
import it.pagopa.selfcare.onboarding.exception.InternalGatewayErrorException;
import it.pagopa.selfcare.onboarding.mapper.DocumentMapper;
import it.pagopa.selfcare.product.entity.AttachmentTemplate;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.document_json.api.DocumentContentControllerApi;
import org.openapi.quarkus.document_json.api.DocumentControllerApi;
import org.openapi.quarkus.document_json.model.DocumentBuilderRequest;
import org.openapi.quarkus.document_json.model.DocumentType;
import org.openapi.quarkus.document_json.model.UserAttachmentRequest;
import java.io.File;
import it.pagopa.selfcare.onboarding.exception.UnauthorizedUserException;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import java.io.IOException;
import jakarta.ws.rs.ProcessingException;
import java.time.temporal.ChronoUnit;

@ApplicationScoped
public class DocumentService {

    private final DocumentContentControllerApi documentContentApi;
    private final DocumentControllerApi documentApi;
    private final DocumentMapper documentMapper;

    public DocumentService(@RestClient DocumentContentControllerApi documentContentApi,
                           @RestClient DocumentControllerApi documentApi,
                           DocumentMapper documentMapper) {
        this.documentContentApi = documentContentApi;
        this.documentApi = documentApi;
        this.documentMapper = documentMapper;
    }

    @Retry(maxRetries = 3, delay = 5000, delayUnit = ChronoUnit.MILLIS, retryOn = {ProcessingException.class, IOException.class}, abortOn = {ResourceNotFoundException.class, InvalidRequestException.class, UnauthorizedUserException.class})
    public BinaryData getContract(String onboardingId) {
        try {
            File file = documentContentApi.getContract(onboardingId).await().indefinitely();
            return FilePayloadUtils.toBinaryData(file, file.getName());
        } catch (Exception e) {
            throw new InternalGatewayErrorException("Error retrieving contract from document service");
        }
    }

    @Retry(maxRetries = 3, delay = 5000, delayUnit = ChronoUnit.MILLIS, retryOn = {ProcessingException.class, IOException.class}, abortOn = {ResourceNotFoundException.class, InvalidRequestException.class, UnauthorizedUserException.class})
    public BinaryData getTemplateAttachment(String onboardingId,
                                            String institutionDescription,
                                            String filename,
                                            String productId,
                                            String templatePath) {
        File file = documentContentApi
                .getTemplateAttachment(onboardingId, institutionDescription, filename, productId, templatePath)
                .await().indefinitely();
        return FilePayloadUtils.toBinaryData(file, filename);
    }

    @Retry(maxRetries = 3, delay = 5000, delayUnit = ChronoUnit.MILLIS, retryOn = {ProcessingException.class, IOException.class}, abortOn = {ResourceNotFoundException.class, InvalidRequestException.class, UnauthorizedUserException.class})
    public BinaryData getAttachment(String onboardingId, String filename) {
        File file = documentContentApi.getAttachment(onboardingId, filename).await().indefinitely();
        return FilePayloadUtils.toBinaryData(file, filename);
    }

    @Retry(maxRetries = 3, delay = 5000, delayUnit = ChronoUnit.MILLIS, retryOn = {ProcessingException.class, IOException.class}, abortOn = {ResourceNotFoundException.class, InvalidRequestException.class, UnauthorizedUserException.class})
    public AvailableDocuments getAvailableDocuments(String onboardingId) {
        return documentMapper.toAvailableDocuments(documentApi.getAvailableDocuments(onboardingId).await().indefinitely());
    }

    @Retry(maxRetries = 3, delay = 5000, delayUnit = ChronoUnit.MILLIS, retryOn = {ProcessingException.class, IOException.class}, abortOn = {ResourceNotFoundException.class, InvalidRequestException.class, UnauthorizedUserException.class})
    public BinaryData getAggregatesCsv(String onboardingId, String productId) {
        File file = documentContentApi.getAggregatesCsv(onboardingId, productId).await().indefinitely();
        return FilePayloadUtils.toBinaryData(file, file.getName());
    }

    @Retry(maxRetries = 3, delay = 5000, delayUnit = ChronoUnit.MILLIS, retryOn = {ProcessingException.class, IOException.class}, abortOn = {ResourceNotFoundException.class, InvalidRequestException.class, UnauthorizedUserException.class})
    public void uploadAttachment(String onboardingId,
                                 UploadedFile attachment,
                                 String attachmentName,
                                 String attachmentId,
                                 String attachmentDescription,
                                 String productId,
                                 AttachmentTemplate template) {
        DocumentBuilderRequest request = new DocumentBuilderRequest();
        request.setAttachmentName(attachmentName);
        request.setProductId(productId);
        request.setOnboardingId(onboardingId);
        request.setTemplatePath(template.getTemplatePath());
        request.setTemplateVersion(template.getTemplateVersion());
        request.setDocumentType(DocumentType.ATTACHMENT);

        DocumentContentControllerApi.UploadAttachmentMultipartForm form =
                new DocumentContentControllerApi.UploadAttachmentMultipartForm();
        form._file = FilePayloadUtils.toTempFile(attachment, "document-attachment-", ".bin");
        form.request = request;
        documentContentApi.uploadAttachment(form).await().indefinitely();
    }

    @Retry(maxRetries = 3, delay = 5000, delayUnit = ChronoUnit.MILLIS, retryOn = {ProcessingException.class, IOException.class}, abortOn = {ResourceNotFoundException.class, InvalidRequestException.class, UnauthorizedUserException.class})
    public void uploadUserAttachment(String onboardingId,
                                     UploadedFile attachment,
                                     String productId,
                                     String attachmentId,
                                     String attachmentDescription,
                                     String attachmentName,
                                     Integer maxDocumentsRequired) {
        UserAttachmentRequest request = new UserAttachmentRequest();
        request.setOnboardingId(onboardingId);
        request.setProductId(productId);
        request.setAttachmentId(attachmentId);
        request.setAttachmentDescription(attachmentDescription);
        request.setAttachmentName(attachmentName);
        request.setMaxDocumentsRequired(maxDocumentsRequired);
        DocumentContentControllerApi.UploadUserAttachmentMultipartForm form =
                new DocumentContentControllerApi.UploadUserAttachmentMultipartForm();
        form._file = FilePayloadUtils.toTempFile(attachment, "document-user-attachment-", ".bin");
        form.request = request;
        documentContentApi.uploadUserAttachment(form).await().indefinitely();
    }

    @Retry(maxRetries = 3, delay = 5000, delayUnit = ChronoUnit.MILLIS, retryOn = {ProcessingException.class, IOException.class}, abortOn = {ResourceNotFoundException.class, InvalidRequestException.class, UnauthorizedUserException.class})
    public int headAttachment(String onboardingId, String filename) {
        return documentApi.headAttachment(onboardingId, filename)
                .await().indefinitely()
                .getStatus();
    }
}
