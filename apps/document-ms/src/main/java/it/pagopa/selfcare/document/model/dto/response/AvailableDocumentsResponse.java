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

    @Schema(description = "Names of the attachments available for the onboarding. Empty list if none.")
    private List<String> attachments;

    @Schema(description = "Filename of the signed contract available for download. Null when the onboarding has no signed contract yet (e.g. state TOBEVALIDATED).")
    private String contractFilename;
}

