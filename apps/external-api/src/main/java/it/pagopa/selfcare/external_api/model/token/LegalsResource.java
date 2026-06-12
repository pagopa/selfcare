package it.pagopa.selfcare.external_api.model.token;

import it.pagopa.selfcare.commons.base.utils.Env;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import lombok.Data;

@Data
public class LegalsResource {

    private String partyId;
    private String relationshipId;
    private PartyRole role;
    private Env env;
}
