package it.pagopa.selfcare.document.model.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Geographic taxonomy data for PDF generation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeographicTaxonomyPdfData {

    private String code;
    private String desc;
}
