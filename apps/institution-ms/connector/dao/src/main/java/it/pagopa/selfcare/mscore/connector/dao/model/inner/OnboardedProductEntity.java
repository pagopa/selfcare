package it.pagopa.selfcare.mscore.connector.dao.model.inner;

import it.pagopa.selfcare.commons.base.security.PartyRole;
import it.pagopa.selfcare.mscore.constant.Env;
import it.pagopa.selfcare.mscore.constant.RelationshipState;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.OffsetDateTime;

import static it.pagopa.selfcare.mscore.constant.Env.ROOT;

@Data
@FieldNameConstants(asEnum = true)
public class OnboardedProductEntity {

    @Indexed
    private String productId;

    private String relationshipId;
    private String tokenId;
    private RelationshipState status;
    private String contract;
    private String productRole;
    private PartyRole role;
    private Env env = ROOT;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
