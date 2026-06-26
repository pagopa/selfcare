package it.pagopa.selfcare.iam.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRolePermissionsList {

  private String userId;
  private String productId;
  private String name;
  private String familyName;
  private String email;
  private List<ProductRolePermissions> items;

}
