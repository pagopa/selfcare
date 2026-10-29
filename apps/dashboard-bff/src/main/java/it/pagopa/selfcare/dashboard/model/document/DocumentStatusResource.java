package it.pagopa.selfcare.dashboard.model.document;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentStatusResource {

    @Schema(description = "${swagger.dashboard.documents.model.products}")
    private List<ProductDocumentStatusResource> products;

}
