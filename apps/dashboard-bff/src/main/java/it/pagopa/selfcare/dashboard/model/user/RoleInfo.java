package it.pagopa.selfcare.dashboard.model.user;

import it.pagopa.selfcare.commons.base.security.SelfCareAuthority;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class RoleInfo {
    private String relationshipId;
    private String role;
    private String status;
    private SelfCareAuthority selcRole;
    private String partyRole;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
