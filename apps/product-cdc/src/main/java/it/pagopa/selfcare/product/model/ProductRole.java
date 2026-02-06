package it.pagopa.selfcare.product.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductRole {
  private String code;
  private String label;
  private String productLabel;
  private String description;
}
