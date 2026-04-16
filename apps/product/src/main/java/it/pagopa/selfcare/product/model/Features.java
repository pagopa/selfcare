package it.pagopa.selfcare.product.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Features {
  private boolean allowCompanyOnboarding;
  private boolean allowIndividualOnboarding;
  private List<String> allowedInstitutionTaxCode;
  private boolean delegable;
  private boolean invoiceable;

  @Builder.Default private int expirationDays = 30;

  private boolean enabled;
}
