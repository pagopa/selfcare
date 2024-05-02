package it.pagopa.selfcare.user.model;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateUserRequest {
    private String name;
    private String familyName;
    @NotNull
    private String email;
}
