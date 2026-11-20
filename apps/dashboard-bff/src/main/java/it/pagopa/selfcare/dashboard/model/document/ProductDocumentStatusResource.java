package it.pagopa.selfcare.dashboard.model.document;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDocumentStatusResource {

    @Schema(description = "${swagger.dashboard.products.model.id}")
    private String productId;

    @Schema(description = "${swagger.dashboard.documents.model.contractUpdateRequired}")
    private Boolean contractUpdateRequired;

    @Schema(description = "${swagger.dashboard.documents.model.contractUpdateInProgress}")
    private Boolean contractUpdateInProgress;

}
