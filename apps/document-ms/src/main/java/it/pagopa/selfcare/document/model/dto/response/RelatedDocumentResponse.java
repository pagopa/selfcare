package it.pagopa.selfcare.document.model.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import it.pagopa.selfcare.onboarding.common.DocumentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RelatedDocumentResponse {

    @Schema(description = "Unique identifier of the document.")
    private String id;

    @Schema(description = "Physical filename of the document as stored.")
    private String fileName;

    @Schema(description = "Document type.")
    private DocumentType type;

    @Schema(description = "MIME type derived from the file extension.")
    private String mimeType;

    @Schema(description = "Timestamp when the document was first created.")
    private LocalDateTime createdAt;

    @Schema(description = "Full storage path of the document.")
    private String filePath;
}
