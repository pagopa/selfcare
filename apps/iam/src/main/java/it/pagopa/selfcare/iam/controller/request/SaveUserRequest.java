package it.pagopa.selfcare.iam.controller.request;

import it.pagopa.selfcare.iam.model.ProductRoles;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
