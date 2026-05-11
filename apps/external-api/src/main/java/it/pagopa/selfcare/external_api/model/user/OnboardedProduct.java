package it.pagopa.selfcare.external_api.model.user;

import it.pagopa.selfcare.commons.base.security.PartyRole;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.time.OffsetDateTime;

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
    private Env env = Env.ROOT;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
