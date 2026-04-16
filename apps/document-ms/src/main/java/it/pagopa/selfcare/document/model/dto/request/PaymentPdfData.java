package it.pagopa.selfcare.document.model.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payment data for PDF generation (PRV/GPU institution types).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentPdfData {

    private String holder;
    private String iban;
}
