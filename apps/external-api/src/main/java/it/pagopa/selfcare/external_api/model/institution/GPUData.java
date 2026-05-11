package it.pagopa.selfcare.external_api.model.institution;

import lombok.Data;

@Data
public class GPUData {
  private String businessRegisterNumber;
  private String legalRegisterNumber;
  private String legalRegisterName;
  private boolean manager;
  private boolean managerAuthorized;
  private boolean managerEligible;
  private boolean managerProsecution;
  private boolean institutionCourtMeasures;
}
