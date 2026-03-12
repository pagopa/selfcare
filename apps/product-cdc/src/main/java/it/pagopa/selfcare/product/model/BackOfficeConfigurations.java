package it.pagopa.selfcare.product.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BackOfficeConfigurations {
  private String url;
  private String identityTokenAudience;
}
