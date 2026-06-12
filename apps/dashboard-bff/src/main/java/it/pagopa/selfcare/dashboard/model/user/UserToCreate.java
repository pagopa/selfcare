package it.pagopa.selfcare.dashboard.model.user;

import it.pagopa.selfcare.onboarding.common.PartyRole;
import lombok.Data;

import java.util.Set;

@Data
public class UserToCreate {
    private String name;
    private String surname;
    private String taxCode;
    private String email;
    private PartyRole role;
    private Set<String> productRoles;
    private Boolean toAddOnAggregates;
}
