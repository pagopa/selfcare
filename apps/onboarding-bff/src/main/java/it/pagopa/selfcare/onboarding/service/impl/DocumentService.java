package it.pagopa.selfcare.onboarding.service.impl;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
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

    // The document content client is synchronous: the call, the body read and the Response close run on a worker
    @Retry(maxRetries = 2, delay = 5000, delayUnit = ChronoUnit.MILLIS, jitter = 0, retryOn = {ProcessingException.class, IOException.class})
    public Uni<BinaryData> getContract(String onboardingId) {
        return Uni.createFrom().item(() -> toBinaryData(documentContentClient.getContract(onboardingId)))
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    @Retry(maxRetries = 2, delay = 5000, delayUnit = ChronoUnit.MILLIS, jitter = 0, retryOn = {ProcessingException.class, IOException.class})
    public Uni<BinaryData> getContractSigned(String onboardingId) {
        return Uni.createFrom().item(() -> toBinaryData(documentContentClient.getContractSigned(onboardingId)))
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    @Retry(maxRetries = 2, delay = 5000, delayUnit = ChronoUnit.MILLIS, jitter = 0, retryOn = {ProcessingException.class, IOException.class})
    public Uni<BinaryData> getTemplateAttachment(String onboardingId,
                                            String institutionDescription,
                                            String filename,
                                            String productId,
                                            String templatePath) {
        return Uni.createFrom().item(() -> toBinaryData(documentContentClient
                        .getTemplateAttachment(onboardingId, institutionDescription, filename, productId, templatePath)))
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    @Retry(maxRetries = 2, delay = 5000, delayUnit = ChronoUnit.MILLIS, jitter = 0, retryOn = {ProcessingException.class, IOException.class})
    public Uni<BinaryData> getAttachment(String onboardingId, String filename) {
        return Uni.createFrom().item(() -> toBinaryData(documentContentClient.getAttachment(onboardingId, filename)))
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    @Retry(maxRetries = 2, delay = 5000, delayUnit = ChronoUnit.MILLIS, jitter = 0, retryOn = {ProcessingException.class, IOException.class})
    public Uni<AvailableDocuments> getAvailableDocuments(String onboardingId) {
        return documentApi.getAvailableDocuments(onboardingId).map(documentMapper::toAvailableDocuments);
    }

    @Retry(maxRetries = 2, delay = 5000, delayUnit = ChronoUnit.MILLIS, jitter = 0, retryOn = {ProcessingException.class, IOException.class})
    public Uni<BinaryData> getAggregatesCsv(String onboardingId, String productId) {
        return Uni.createFrom().item(() -> toBinaryData(documentContentClient.getAggregatesCsv(onboardingId, productId)))
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    public Uni<Void> uploadAttachment(String onboardingId,
                                 UploadedFile attachment,
                                 String attachmentName,
                                 String productId,
                                 AttachmentTemplate template) {
        DocumentBuilderRequest request = documentMapper.toDocumentBuilderRequest(
                onboardingId, attachmentName, productId, template);
        return Uni.createFrom().voidItem()
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                .invoke(() -> documentContentClient.uploadAttachment(multipart(attachment, request, DocumentBuilderRequest.class)).close());
    }

    public Uni<Void> uploadUserAttachment(String onboardingId,
                                     UploadedFile attachment,
                                     String productId,
                                     String attachmentId,
                                     String attachmentDescription,
                                     String attachmentName,
                                     Integer maxDocumentsRequired) {
        UserAttachmentRequest request = documentMapper.toUserAttachmentRequest(
                onboardingId, productId, attachmentId, attachmentDescription, attachmentName, maxDocumentsRequired);
        return Uni.createFrom().voidItem()
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                .invoke(() -> documentContentClient.uploadUserAttachment(multipart(attachment, request, UserAttachmentRequest.class)).close());
    }

    public Uni<Integer> headAttachment(String onboardingId, String filename) {
        return documentApi.headAttachment(onboardingId, filename).map(DocumentService::statusAndClose);
    }

    private static ClientMultipartForm multipart(UploadedFile attachment, Object request, Class<?> requestType) {
        String contentType = attachment.contentType() == null ? MediaType.APPLICATION_OCTET_STREAM : attachment.contentType();
        return ClientMultipartForm.create()
                .binaryFileUpload(FILE_PART, attachment.fileName(), Buffer.buffer(attachment.content()), contentType)
                .entity(REQUEST_PART, request, MediaType.APPLICATION_JSON, requestType);
    }

    private static int statusAndClose(Response response) {
        try (response) {
            return response.getStatus();
        }
    }

    private static BinaryData toBinaryData(Response response) {
        try (response) {
            byte[] content = response.hasEntity() ? response.readEntity(byte[].class) : new byte[0];
            return new BinaryData(ContentDispositions.filename(response.getHeaderString(HttpHeaders.CONTENT_DISPOSITION)), content);
        }
    }
}
