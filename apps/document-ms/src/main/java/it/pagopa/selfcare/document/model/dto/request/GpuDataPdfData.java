package it.pagopa.selfcare.document.model.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * GPU data for PDF generation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GpuDataPdfData {

    private String businessRegisterNumber;
    private String legalRegisterNumber;
    private String legalRegisterName;
    private boolean longTermPayments;
    private boolean manager;
    private boolean managerAuthorized;
    private boolean managerEligible;
    private boolean managerProsecution;
    private boolean institutionCourtMeasures;
}
