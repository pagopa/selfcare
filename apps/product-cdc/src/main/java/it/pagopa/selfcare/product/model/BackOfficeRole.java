package it.pagopa.selfcare.product.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackOfficeRole {
  private String code;
  private String label;
  private String description;
  private String productLabel;
  private List<String> multiroleGroups;
}
