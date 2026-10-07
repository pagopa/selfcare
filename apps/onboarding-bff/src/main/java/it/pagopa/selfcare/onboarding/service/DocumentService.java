package it.pagopa.selfcare.onboarding.service;

import io.vertx.core.buffer.Buffer;
import it.pagopa.selfcare.onboarding.client.DocumentContentRestClient;
import it.pagopa.selfcare.onboarding.client.model.AttachmentTemplate;
import it.pagopa.selfcare.onboarding.client.model.AvailableDocuments;
import it.pagopa.selfcare.onboarding.client.model.BinaryData;
import it.pagopa.selfcare.onboarding.client.model.UploadedFile;
import it.pagopa.selfcare.onboarding.client.util.ContentDispositions;
import it.pagopa.selfcare.onboarding.mapper.DocumentMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.time.temporal.ChronoUnit;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.client.api.ClientMultipartForm;
import org.openapi.quarkus.document_json.api.DocumentControllerApi;
import org.openapi.quarkus.document_json.model.DocumentBuilderRequest;
import org.openapi.quarkus.document_json.model.DocumentType;
import org.openapi.quarkus.document_json.model.UserAttachmentRequest;

/**
 * Document-ms facade. Reads are retried (3 attempts, 5s apart) on connection problems and timeouts only;
 * uploads and the head check are never retried.
 */
@ApplicationScoped
public class DocumentService {

    private static final String FILE_PART = "file";
    private static final String REQUEST_PART = "request";

    private final DocumentContentRestClient documentContentClient;
    private final DocumentControllerApi documentApi;
    private final DocumentMapper documentMapper;

    public DocumentService(@RestClient DocumentContentRestClient documentContentClient,
                           @RestClient DocumentControllerApi documentApi,
                           DocumentMapper documentMapper) {
        this.documentContentClient = documentContentClient;
        this.documentApi = documentApi;
        this.documentMapper = documentMapper;
    }

    @Retry(maxRetries = 2, delay = 5000, delayUnit = ChronoUnit.MILLIS, jitter = 0, retryOn = {ProcessingException.class, IOException.class})
    public BinaryData getContract(String onboardingId) {
        return toBinaryData(documentContentClient.getContract(onboardingId));
    }

    @Retry(maxRetries = 2, delay = 5000, delayUnit = ChronoUnit.MILLIS, jitter = 0, retryOn = {ProcessingException.class, IOException.class})
    public BinaryData getContractSigned(String onboardingId) {
        return toBinaryData(documentContentClient.getContractSigned(onboardingId));
    }

    @Retry(maxRetries = 2, delay = 5000, delayUnit = ChronoUnit.MILLIS, jitter = 0, retryOn = {ProcessingException.class, IOException.class})
    public BinaryData getTemplateAttachment(String onboardingId,
                                            String institutionDescription,
                                            String filename,
                                            String productId,
                                            String templatePath) {
        return toBinaryData(documentContentClient
                .getTemplateAttachment(onboardingId, institutionDescription, filename, productId, templatePath));
    }

    @Retry(maxRetries = 2, delay = 5000, delayUnit = ChronoUnit.MILLIS, jitter = 0, retryOn = {ProcessingException.class, IOException.class})
    public BinaryData getAttachment(String onboardingId, String filename) {
        return toBinaryData(documentContentClient.getAttachment(onboardingId, filename));
    }

    @Retry(maxRetries = 2, delay = 5000, delayUnit = ChronoUnit.MILLIS, jitter = 0, retryOn = {ProcessingException.class, IOException.class})
    public AvailableDocuments getAvailableDocuments(String onboardingId) {
        return documentMapper.toAvailableDocuments(documentApi.getAvailableDocuments(onboardingId).await().indefinitely());
    }

    @Retry(maxRetries = 2, delay = 5000, delayUnit = ChronoUnit.MILLIS, jitter = 0, retryOn = {ProcessingException.class, IOException.class})
    public BinaryData getAggregatesCsv(String onboardingId, String productId) {
        return toBinaryData(documentContentClient.getAggregatesCsv(onboardingId, productId));
    }

    public void uploadAttachment(String onboardingId,
                                 UploadedFile attachment,
                                 String attachmentName,
                                 String productId,
                                 AttachmentTemplate template) {
        DocumentBuilderRequest request = new DocumentBuilderRequest();
        request.setAttachmentName(attachmentName);
        request.setProductId(productId);
        request.setOnboardingId(onboardingId);
        request.setTemplatePath(template.getTemplatePath());
        request.setTemplateVersion(template.getTemplateVersion());
        request.setDocumentType(DocumentType.ATTACHMENT);
        documentContentClient.uploadAttachment(multipart(attachment, request, DocumentBuilderRequest.class)).close();
    }

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
        documentContentClient.uploadUserAttachment(multipart(attachment, request, UserAttachmentRequest.class)).close();
    }

    public int headAttachment(String onboardingId, String filename) {
        try (Response response = documentApi.headAttachment(onboardingId, filename).await().indefinitely()) {
            return response.getStatus();
        }
    }

    private static ClientMultipartForm multipart(UploadedFile attachment, Object request, Class<?> requestType) {
        String contentType = attachment.contentType() == null ? MediaType.APPLICATION_OCTET_STREAM : attachment.contentType();
        return ClientMultipartForm.create()
                .binaryFileUpload(FILE_PART, attachment.fileName(), Buffer.buffer(attachment.content()), contentType)
                .entity(REQUEST_PART, request, MediaType.APPLICATION_JSON, requestType);
    }

    private static BinaryData toBinaryData(Response response) {
        try (response) {
            byte[] content = response.hasEntity() ? response.readEntity(byte[].class) : new byte[0];
            return new BinaryData(ContentDispositions.filename(response.getHeaderString(HttpHeaders.CONTENT_DISPOSITION)), content);
        }
    }
}
