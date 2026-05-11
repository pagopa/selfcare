package it.pagopa.selfcare.external_api.model.token;

import it.pagopa.selfcare.onboarding.common.PartyRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TokenUser {
    private String userId;
    private PartyRole role;
}
