package it.pagopa.selfcare.document.model.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Billing data for PDF generation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingPdfData {

    private String vatNumber;
    private String recipientCode;
    private boolean publicServices;
    private String taxCodeInvoicing;
}
