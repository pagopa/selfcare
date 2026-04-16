package it.pagopa.selfcare.mscore.model.onboarding;

import it.pagopa.selfcare.commons.base.security.PartyRole;
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
