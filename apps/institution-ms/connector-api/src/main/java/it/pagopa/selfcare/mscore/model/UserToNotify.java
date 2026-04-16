package it.pagopa.selfcare.mscore.model;

import it.pagopa.selfcare.commons.base.security.PartyRole;
import it.pagopa.selfcare.mscore.constant.RelationshipState;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserToNotify {

    private String userId;
    private String name;
    private String familyName;
    private String email;
    private PartyRole role;
    private String productRole;
    private RelationshipState relationshipStatus;

}
