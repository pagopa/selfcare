package it.pagopa.selfcare.dashboard.model.user;

import it.pagopa.selfcare.onboarding.common.PartyRole;
import lombok.Data;

import java.util.Set;

@Data
public class CreateUserDto {

    private String name;
    private String surname;
    private String taxCode;
    private String email;
    private Set<Role> roles;
    private SaveUserDto user;

    @Data
    public static class Role {
        private String productRole;
        private String label;
        private PartyRole partyRole;
    }
}