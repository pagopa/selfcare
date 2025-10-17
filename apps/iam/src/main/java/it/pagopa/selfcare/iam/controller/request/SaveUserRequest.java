package it.pagopa.selfcare.iam.controller.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import jakarta.validation.constraints.Email;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class SaveUserRequest {

  @NotEmpty(message = "email is required")
  @NotNull(message = "email is required")
  @Email
  private String email;
  private String name;
  private String familyName;
  private List<String> roles;
}
