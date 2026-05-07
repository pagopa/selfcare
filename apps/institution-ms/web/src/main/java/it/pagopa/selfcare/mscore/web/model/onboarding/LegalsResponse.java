package it.pagopa.selfcare.mscore.web.model.onboarding;

import it.pagopa.selfcare.commons.base.security.PartyRole;
import it.pagopa.selfcare.mscore.constant.Env;
import lombok.Data;

@Data
public class LegalsResponse {
    private String partyId;
    private String relationshipId;
    private PartyRole role;
    private Env env;
}
