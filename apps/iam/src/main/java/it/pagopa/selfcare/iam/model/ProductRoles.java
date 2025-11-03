package it.pagopa.selfcare.product.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Model representing roles assigned to a user for a specific product.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRoles {
  String productId;
  List<String> roles;
}
