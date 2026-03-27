package it.pagopa.selfcare.document.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for PDF creation operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePdfResponse {

    /**
     * Path where the PDF was stored in Azure Blob Storage.
     */
    private String storagePath;

    /**
     * Filename of the generated PDF.
     */
    private String filename;
}
