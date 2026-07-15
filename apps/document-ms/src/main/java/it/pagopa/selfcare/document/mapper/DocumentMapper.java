package it.pagopa.selfcare.document.mapper;

import it.pagopa.selfcare.document.model.dto.response.DocumentResponse;
import it.pagopa.selfcare.document.model.dto.response.RelatedDocumentResponse;
import it.pagopa.selfcare.document.model.entity.Document;
import org.mapstruct.Mapper;

import java.util.Locale;
import java.util.Objects;

@Mapper(componentModel = "cdi")
public interface DocumentMapper {

    DocumentResponse toResponse(Document entity);

    default RelatedDocumentResponse toRelatedDocumentResponse(Document entity, String filePath) {
        return RelatedDocumentResponse.builder()
                .id(entity.getId())
                .name(resolveName(entity))
                .fileName(extractFileName(filePath))
                .type(Objects.isNull(entity.getType()) ? null : entity.getType().name().toLowerCase(Locale.ROOT))
                .mimeType(resolveMimeType(filePath))
                .createdAt(entity.getCreatedAt())
                .filePath(filePath)
                .build();
    }

    private static String resolveName(Document entity) {
        if (Objects.nonNull(entity.getAttachmentName()) && !entity.getAttachmentName().isBlank()) {
            return entity.getAttachmentName();
        }
        return Objects.isNull(entity.getType()) ? null : entity.getType().name().toLowerCase(Locale.ROOT);
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
