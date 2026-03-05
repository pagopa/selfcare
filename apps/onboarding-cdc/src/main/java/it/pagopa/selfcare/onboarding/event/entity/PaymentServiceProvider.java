package it.pagopa.selfcare.onboarding.event.entity;

import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PaymentServiceProvider extends BusinessData {
  private String abiCode;
  private boolean vatNumberGroup;
  private String contractType;
  private String contractId;
  private List<String> providerNames;
}
