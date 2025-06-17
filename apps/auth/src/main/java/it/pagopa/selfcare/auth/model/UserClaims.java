package it.pagopa.selfcare.auth.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class UserClaims {
    private String uid;
    private String fiscalCode;
    private String name;
    private String familyName;
    private Boolean sameIdp = Boolean.TRUE;
}
