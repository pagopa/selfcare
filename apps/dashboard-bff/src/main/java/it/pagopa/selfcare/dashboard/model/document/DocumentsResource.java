package it.pagopa.selfcare.dashboard.model.document;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentsResource {

    @Schema(description = "${swagger.dashboard.documents.model.documents}")
    private List<DocumentResource> documents;

}
