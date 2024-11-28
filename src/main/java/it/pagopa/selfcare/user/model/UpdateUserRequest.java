package it.pagopa.selfcare.user.model;

import lombok.Data;

import javax.validation.constraints.Pattern;

@Data
public class UpdateUserRequest {
    private String name;
    private String familyName;
    private String email;
    @Pattern(regexp = "^\\+?[0-9]{9,15}$", message = "Il numero di telefono non è valido")
    private String mobilePhone;
}
