package it.pagopa.selfcare.onboarding.controller.response;

import it.pagopa.selfcare.onboarding.common.PartyRole;
import lombok.Data;

@Data
public class UserOnboardingResponse {

    private String id;
    private PartyRole role;
    private String productRole;
    private String userMailUuid;
}
