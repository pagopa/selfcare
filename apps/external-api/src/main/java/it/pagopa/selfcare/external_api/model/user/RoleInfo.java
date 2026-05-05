package it.pagopa.selfcare.external_api.model.user;

import it.pagopa.selfcare.commons.base.security.SelfCareAuthority;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoleInfo {
    private String relationshipId;
    private String role;
    private String status;
    private SelfCareAuthority selcRole;
}
