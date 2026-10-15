package it.pagopa.selfcare.document.model.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

/**
 * Aggregated view of the documents available for download for a given onboarding.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AvailableDocumentsResponse {

    @Schema(description = "Documents of the dossier available for download. For an institution onboarding: its signed contract, "
            + "the signed contracts of the update onboardings, the attachments and the signed USER contracts linked to it. "
            + "For a USER onboarding: only its signed contract. Empty list if none.")
    private List<AvailableDocumentResponse> documents;
}

