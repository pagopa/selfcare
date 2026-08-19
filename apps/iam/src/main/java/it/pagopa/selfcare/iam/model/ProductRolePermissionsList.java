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
public class ProductRolePermissionsList {

  private String userId;
  private String productId;
  private String tenantId;
  private String name;
  private String familyName;
  private String email;
  private List<ProductRolePermissions> items;

}
