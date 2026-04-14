package it.pagopa.selfcare.onboarding.service;

import it.pagopa.selfcare.onboarding.client.model.BinaryData;
import it.pagopa.selfcare.onboarding.client.model.UploadedFile;
import it.pagopa.selfcare.onboarding.client.util.FilePayloadUtils;
import it.pagopa.selfcare.product.entity.AttachmentTemplate;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.document_json.api.DocumentContentControllerApi;
import org.openapi.quarkus.document_json.api.DocumentControllerApi;
import org.openapi.quarkus.document_json.model.DocumentBuilderRequest;
import org.openapi.quarkus.document_json.model.DocumentType;

import java.io.File;

@ApplicationScoped
public class DocumentService {

    private final DocumentContentControllerApi documentContentApi;
    private final DocumentControllerApi documentApi;

    public DocumentService(@RestClient DocumentContentControllerApi documentContentApi,
                            @RestClient DocumentControllerApi documentApi) {
        this.documentContentApi = documentContentApi;
        this.documentApi = documentApi;
    }

    @Retry(maxRetries = 2, delay = 5000)
    public BinaryData getContract(String onboardingId) {
        File file = documentContentApi.getContract(onboardingId).await().indefinitely();
        return FilePayloadUtils.toBinaryData(file, file.getName());
    }

    @Retry(maxRetries = 2, delay = 5000)
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

    @Retry(maxRetries = 2, delay = 5000)
    public BinaryData getAttachment(String onboardingId, String filename) {
        File file = documentContentApi.getAttachment(onboardingId, filename).await().indefinitely();
        return FilePayloadUtils.toBinaryData(file, filename);
    }

    @Retry(maxRetries = 2, delay = 5000)
    public BinaryData getAggregatesCsv(String onboardingId, String productId) {
        File file = documentContentApi.getAggregatesCsv(onboardingId, productId).await().indefinitely();
        return FilePayloadUtils.toBinaryData(file, file.getName());
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

        DocumentContentControllerApi.UploadAttachmentMultipartForm form =
                new DocumentContentControllerApi.UploadAttachmentMultipartForm();
        form._file = FilePayloadUtils.toTempFile(attachment, "document-attachment-", ".bin");
        form.request = request;
        documentContentApi.uploadAttachment(form).await().indefinitely();
    }

    public int headAttachment(String onboardingId, String filename) {
        return documentApi.headAttachment(onboardingId, filename)
                .await().indefinitely()
                .getStatus();
    }
}
