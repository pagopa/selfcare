package it.pagopa.selfcare.onboarding.controller.request;

import it.pagopa.selfcare.onboarding.entity.BusinessData;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PaymentServiceProviderRequest extends BusinessData {
  private String abiCode;
  private boolean vatNumberGroup;
  private List<String> providerNames;
  private String contractType;
  private String contractId;
}

