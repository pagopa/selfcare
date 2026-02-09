package it.pagopa.selfcare.iam.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRolePermissions {
  private String productId;
  private String role;
  private List<String> permissions;
}
