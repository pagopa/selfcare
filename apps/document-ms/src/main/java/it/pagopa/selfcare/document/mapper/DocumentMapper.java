package it.pagopa.selfcare.document.mapper;

import it.pagopa.selfcare.document.model.dto.response.AvailableDocumentResponse;
import it.pagopa.selfcare.document.model.dto.response.DocumentResponse;
import it.pagopa.selfcare.document.model.dto.response.RelatedDocumentResponse;
import it.pagopa.selfcare.document.model.entity.Document;
import org.mapstruct.Mapper;

import java.util.Objects;

import static it.pagopa.selfcare.onboarding.common.DocumentType.ATTACHMENT;

@Mapper(componentModel = "cdi")
public interface DocumentMapper {

    DocumentResponse toResponse(Document entity);

    default RelatedDocumentResponse toRelatedDocumentResponse(Document entity, String filePath) {
        return RelatedDocumentResponse.builder()
                .id(entity.getId())
                .fileName(extractFileName(filePath))
                .type(entity.getType())
                .mimeType(resolveMimeType(filePath))
                .createdAt(entity.getCreatedAt())
                .filePath(filePath)
                .build();
    }

    default AvailableDocumentResponse toAvailableDocumentResponse(Document entity) {
        return AvailableDocumentResponse.builder()
                .name(ATTACHMENT.equals(entity.getType()) ? entity.getAttachmentName() : entity.getContractFilename())
                .type(entity.getType())
                .onboardingId(entity.getOnboardingId())
                .rootOnboardingId(entity.getRootOnboardingId())
                .build();
    }

    private static String extractFileName(String path) {
        if (Objects.isNull(path) || path.isBlank()) {
            return null;
        }
        int lastSlash = path.lastIndexOf('/');
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    }

    private static String resolveMimeType(String path) {
        if (Objects.isNull(path) || path.isBlank()) {
            return "application/octet-stream";
        }
        String normalizedPath = path.toLowerCase();
        if (normalizedPath.endsWith(".p7m")) {
            return "application/pkcs7-mime";
        }
        if (normalizedPath.endsWith(".pdf")) {
            return "application/pdf";
        }
        return "application/octet-stream";
    }
}
