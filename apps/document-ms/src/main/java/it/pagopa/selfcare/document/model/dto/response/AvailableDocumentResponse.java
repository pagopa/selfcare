package it.pagopa.selfcare.document.model.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import it.pagopa.selfcare.onboarding.common.DocumentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Single document of the onboarding dossier available for download.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AvailableDocumentResponse {

    @Schema(description = "Document name: contractFilename for INSTITUTION/USER documents, attachmentName for ATTACHMENT documents.")
    private String name;

    @Schema(description = "Document type.")
    private DocumentType type;

    @Schema(description = "Identifier of the onboarding the document belongs to.")
    private String onboardingId;

    @Schema(description = "Identifier of the root onboarding (institution adhesion) of the document dossier.")
    private String rootOnboardingId;
}

