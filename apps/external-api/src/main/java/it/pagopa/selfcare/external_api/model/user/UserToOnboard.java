package it.pagopa.selfcare.external_api.model.user;

import it.pagopa.selfcare.commons.base.security.PartyRole;
import lombok.Data;

@Data
public class UserToOnboard {

    private String id;
    private String taxCode;
    private String name;
    private String surname;
    private String email;
    private String userMailUuid;
    private PartyRole role;
    private String productRole;
    private String roleLabel;
    private Env env;

}
