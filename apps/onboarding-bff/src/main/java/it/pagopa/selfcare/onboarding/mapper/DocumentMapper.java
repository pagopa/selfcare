package it.pagopa.selfcare.onboarding.mapper;

import it.pagopa.selfcare.onboarding.client.model.AvailableDocuments;
import it.pagopa.selfcare.onboarding.client.model.AttachmentTemplate;
import org.mapstruct.Mapper;
import org.openapi.quarkus.document_json.model.DocumentBuilderRequest;
import org.openapi.quarkus.document_json.model.DocumentType;
import org.openapi.quarkus.document_json.model.UserAttachmentRequest;

@Mapper(componentModel = "cdi")
public interface DocumentMapper {
    AvailableDocuments toAvailableDocuments(org.openapi.quarkus.document_json.model.AvailableDocumentsResponse response);

    default DocumentBuilderRequest toDocumentBuilderRequest(String onboardingId, String attachmentName,
                                                           String productId, AttachmentTemplate template) {
        DocumentBuilderRequest request = new DocumentBuilderRequest();
        request.setAttachmentName(attachmentName);
        request.setProductId(productId);
        request.setOnboardingId(onboardingId);
        request.setTemplatePath(template.getTemplatePath());
        request.setTemplateVersion(template.getTemplateVersion());
        request.setDocumentType(DocumentType.ATTACHMENT);
        return request;
    }

    default UserAttachmentRequest toUserAttachmentRequest(String onboardingId, String productId,
                                                         String attachmentId, String attachmentDescription,
                                                         String attachmentName, Integer maxDocumentsRequired) {
        UserAttachmentRequest request = new UserAttachmentRequest();
        request.setOnboardingId(onboardingId);
        request.setProductId(productId);
        request.setAttachmentId(attachmentId);
        request.setAttachmentDescription(attachmentDescription);
        request.setAttachmentName(attachmentName);
        request.setMaxDocumentsRequired(maxDocumentsRequired);
        return request;
    }
}
