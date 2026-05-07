package it.pagopa.selfcare.dashboard.model.user;

import it.pagopa.selfcare.commons.base.security.PartyRole;
import it.pagopa.selfcare.commons.base.utils.Env;
import it.pagopa.selfcare.dashboard.model.institution.RelationshipState;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OnboardedProduct {

    private String productId;
    private String tokenId;
    private RelationshipState status;
    private String productRole;
    private PartyRole role;
    private Env env;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}