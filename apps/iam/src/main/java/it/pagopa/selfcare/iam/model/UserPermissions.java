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
public class UserPermissions {
  private String email;
  private String uid;
  private String productId;
  private List<String> permissions;
}