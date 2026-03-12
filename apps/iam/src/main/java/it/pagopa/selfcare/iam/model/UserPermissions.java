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
public class UserPermissions {
  private String email;
  private String uid;
  private String productId;
  private List<String> permissions;
}
