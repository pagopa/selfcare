package it.pagopa.selfcare.onboarding.controller.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Aggregated list of documents available for download for a given onboarding.")
public class AvailableDocumentsResource {
    @Schema(description = "Names of the attachments available for the onboarding. Empty list if none.")
    private List<String> attachments;

    @Schema(description = "Filename of the signed contract available for download. Null when the onboarding has no signed contract yet (e.g. state TOBEVALIDATED).")
    private String contractFilename;
}
