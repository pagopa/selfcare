package it.pagopa.selfcare.user.model;

import lombok.Data;

@Data
public class UpdateUserRequest {
    private String name;
    private String familyName;
    private String email;
}
