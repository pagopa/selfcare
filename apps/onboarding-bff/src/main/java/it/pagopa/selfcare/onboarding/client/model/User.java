package it.pagopa.selfcare.onboarding.client.model;

import it.pagopa.selfcare.onboarding.common.PartyRole;
import lombok.Data;

@Data
public class User {

    private String id;
    private String name;
    private String surname;
    private String taxCode;
    private PartyRole role;
    private String email;
    private String productRole;

}
