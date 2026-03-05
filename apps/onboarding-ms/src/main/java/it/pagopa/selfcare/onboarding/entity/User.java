package it.pagopa.selfcare.onboarding.entity;

import it.pagopa.selfcare.onboarding.common.PartyRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User {

    private String id;
    private PartyRole role;
    private String productRole;
    private String userMailUuid;
}
