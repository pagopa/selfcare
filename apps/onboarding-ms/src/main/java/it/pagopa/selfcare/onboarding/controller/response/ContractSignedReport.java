package it.pagopa.selfcare.onboarding.controller.response;

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
