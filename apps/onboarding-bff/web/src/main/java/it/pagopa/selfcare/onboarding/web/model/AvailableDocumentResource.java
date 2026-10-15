package it.pagopa.selfcare.onboarding.web.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Document of the onboarding dossier available for download.")
public class AvailableDocumentResource {

    @Schema(description = "Document name: contractFilename for INSTITUTION/USER documents, attachmentName for ATTACHMENT documents.")
    private String name;

    @Schema(description = "Document type (INSTITUTION, USER, ATTACHMENT).")
    private String type;

    @Schema(description = "Identifier of the onboarding the document belongs to.")
    private String onboardingId;

    @Schema(description = "Identifier of the root onboarding (institution adhesion) of the document dossier.")
    private String rootOnboardingId;
}

