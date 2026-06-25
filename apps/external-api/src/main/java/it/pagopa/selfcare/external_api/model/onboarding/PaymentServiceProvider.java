package it.pagopa.selfcare.external_api.model.onboarding;

import lombok.Data;

import java.util.List;

@Data
public class PaymentServiceProvider {

  private String abiCode;
  private String businessRegisterNumber;
  private String legalRegisterName;
  private String legalRegisterNumber;
  private Boolean vatNumberGroup;
  private String contractType;
  private String contractId;
  private List<String> providerNames;
}
