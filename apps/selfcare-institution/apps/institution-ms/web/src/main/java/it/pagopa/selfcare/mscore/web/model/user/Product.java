package it.pagopa.selfcare.mscore.web.model.user;

import it.pagopa.selfcare.commons.base.security.PartyRole;
import it.pagopa.selfcare.mscore.constant.Env;
import it.pagopa.selfcare.mscore.constant.RelationshipState;
import lombok.Data;

import java.time.OffsetDateTime;

import static it.pagopa.selfcare.mscore.constant.Env.ROOT;

@Data
public class Product {

    private String productId;
    private String tokenId;
    private RelationshipState status;
    private String contract;
    private String productRole;
    private PartyRole role;
    private Env env = ROOT;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
