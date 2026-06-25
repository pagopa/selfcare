package it.pagopa.selfcare.mscore.model.onboarding;

import it.pagopa.selfcare.commons.base.security.PartyRole;
import it.pagopa.selfcare.mscore.constant.Env;
import it.pagopa.selfcare.mscore.constant.RelationshipState;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.time.OffsetDateTime;

import static it.pagopa.selfcare.mscore.constant.Env.ROOT;

@Data
@FieldNameConstants(asEnum = true)
public class OnboardedProduct {

    private String relationshipId;
    private String productId;
    private RelationshipState status;
    private String contract;
    private String productRole;
    private PartyRole role;
    private String tokenId;
    private Env env = ROOT;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
