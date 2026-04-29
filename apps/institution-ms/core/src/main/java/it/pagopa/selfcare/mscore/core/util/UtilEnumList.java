package it.pagopa.selfcare.mscore.core.util;

import it.pagopa.selfcare.commons.base.security.PartyRole;
import it.pagopa.selfcare.mscore.constant.RelationshipState;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.NONE)
public class UtilEnumList {

    public static final List<RelationshipState> PRODUCT_RELATIONSHIP_STATES =
            List.of(RelationshipState.PENDING,
                    RelationshipState.REJECTED,
                    RelationshipState.TOBEVALIDATED);
    public static final List<RelationshipState> VALID_RELATIONSHIP_STATES =
            List.of(RelationshipState.ACTIVE,
                    RelationshipState.SUSPENDED);
    public static final List<RelationshipState> ONBOARDING_INFO_DEFAULT_RELATIONSHIP_STATES =
            List.of(RelationshipState.ACTIVE,
                    RelationshipState.PENDING);

    public static final List<RelationshipState> VERIFY_TOKEN_RELATIONSHIP_STATES =
            List.of(RelationshipState.TOBEVALIDATED,
                    RelationshipState.PENDING);

    public static final List<PartyRole> ADMIN_PARTY_ROLE =
            List.of(PartyRole.MANAGER,
                    PartyRole.SUB_DELEGATE,
                    PartyRole.DELEGATE);
}
