package it.pagopa.selfcare.external_api.model.token;

import it.pagopa.selfcare.commons.base.security.PartyRole;
import it.pagopa.selfcare.commons.base.utils.Env;
import it.pagopa.selfcare.external_api.model.user.RelationshipState;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProductToken {
    private String contract;
    private LocalDateTime createdAt;
    private Env env;
    private String productId;
    private String productRole;
    private PartyRole role;
    private RelationshipState status;
    private String tokenId;
    private LocalDateTime updatedAt;
}
