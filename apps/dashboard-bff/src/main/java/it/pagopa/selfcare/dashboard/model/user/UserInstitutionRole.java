package it.pagopa.selfcare.dashboard.model.user;

import lombok.Data;

@Data
public class UserInstitutionRole {
    private String id;
    private String name;
    private String surname;
    private String fiscalCode;
    private String email;
    private String partyRole;
    private String status;
}
