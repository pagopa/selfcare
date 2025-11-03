package it.pagopa.selfcare.product.controller.request;

import it.pagopa.selfcare.product.model.ProductRoles;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class SaveUserRequest {

  @NotEmpty(message = "email is required")
  @NotNull(message = "email is required")
  @Email
  private String email;
  private String uid;
  private String name;
  private String familyName;
  private List<ProductRoles> productRoles;
}
