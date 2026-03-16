package it.pagopa.selfcare.document.controller.response;

import lombok.Data;

@Data
public class ContractSignedReport {
  private boolean cades;

  public static ContractSignedReport cades(boolean status) {
    ContractSignedReport contractSignedReport = new ContractSignedReport();
    contractSignedReport.setCades(status);
    return contractSignedReport;
  }
}
