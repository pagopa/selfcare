package it.pagopa.selfcare.external_api.model.user;

import it.pagopa.selfcare.commons.base.security.PartyRole;
import lombok.Data;

import java.util.List;

@Data
public class UserProductResponse {

    private User user;
    private List<String> roles;
    private PartyRole role;
    private String userMailUuid;
}
