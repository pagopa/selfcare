package it.pagopa.selfcare.onboarding.controller.response;

import it.pagopa.selfcare.onboarding.common.PartyRole;
import lombok.Data;

@Data
public class UserResponse {

    private String id;
    private String taxCode;
    private String name;
    private String surname;
    private String email;
    private PartyRole role;
    private String productRole;
}
