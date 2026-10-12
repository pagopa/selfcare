package it.pagopa.selfcare.onboarding.mapper;

import static org.junit.jupiter.api.Assertions.*;

import it.pagopa.selfcare.onboarding.client.model.AttachmentTemplate;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.openapi.quarkus.document_json.model.DocumentType;

class DocumentMapperTest {

    private final DocumentMapper mapper = new DocumentMapperImpl();

    @Test
    void documentBuilderRequest_preservesEveryFieldAndAttachmentType() {
        AttachmentTemplate template = new AttachmentTemplate();
        template.setTemplatePath("template-path");
        template.setTemplateVersion("template-version");

        var request = mapper.toDocumentBuilderRequest("onboarding", "attachment", "product", template);

        assertEquals("onboarding", request.getOnboardingId());
        assertEquals("attachment", request.getAttachmentName());
        assertEquals("product", request.getProductId());
        assertEquals("template-path", request.getTemplatePath());
        assertEquals("template-version", request.getTemplateVersion());
        assertEquals(DocumentType.ATTACHMENT, request.getDocumentType());
    }

    @Test
    void documentBuilderRequest_preservesNullFieldsButDoesNotHideMissingTemplate() {
        var request = mapper.toDocumentBuilderRequest(null, null, null, new AttachmentTemplate());

        assertNull(request.getOnboardingId());
        assertNull(request.getAttachmentName());
        assertNull(request.getProductId());
        assertNull(request.getTemplatePath());
        assertNull(request.getTemplateVersion());
        assertEquals(DocumentType.ATTACHMENT, request.getDocumentType());
        assertThrows(NullPointerException.class, () -> mapper.toDocumentBuilderRequest(null, null, null, null));
    }

    @Test
    void userAttachmentRequest_preservesEveryFieldAndDoesNotDefaultTheDocumentLimit() {
        for (Integer maxDocuments : Arrays.asList(null, 0, 2)) {
            var request = mapper.toUserAttachmentRequest(
                    "onboarding", "product", "attachment-id", "description", "attachment-name", maxDocuments);

            assertEquals("onboarding", request.getOnboardingId());
            assertEquals("product", request.getProductId());
            assertEquals("attachment-id", request.getAttachmentId());
            assertEquals("description", request.getAttachmentDescription());
            assertEquals("attachment-name", request.getAttachmentName());
            assertEquals(maxDocuments, request.getMaxDocumentsRequired());
        }
    }
}
