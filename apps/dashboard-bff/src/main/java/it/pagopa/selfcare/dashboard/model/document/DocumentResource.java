package it.pagopa.selfcare.dashboard.model.document;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResource {

    @Schema(description = "${swagger.dashboard.documents.model.name}")
    private String name;

    @Schema(description = "${swagger.dashboard.documents.model.type}")
    private DocumentType type;

}
