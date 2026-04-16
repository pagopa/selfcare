package it.pagopa.selfcare.iam.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Model representing roles assigned to a user for a specific product. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRoles {
  String productId;
  List<String> roles;
}
