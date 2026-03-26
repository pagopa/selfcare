package it.pagopa.selfcare.document.model.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Protection Officer data for PDF generation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataProtectionOfficerPdfData {

    private String address;
    private String email;
    private String pec;
}
