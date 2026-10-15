package it.pagopa.selfcare.onboarding.web.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Aggregated list of documents available for download for a given onboarding.")
public class AvailableDocumentsResource {

    @Schema(description = "Documents of the dossier available for download. For an institution onboarding: its signed contract, "
            + "the signed contracts of the update onboardings, the attachments and the signed USER contracts linked to it. "
            + "For a USER onboarding: only its signed contract.")
    private List<AvailableDocumentResource> documents;
}

