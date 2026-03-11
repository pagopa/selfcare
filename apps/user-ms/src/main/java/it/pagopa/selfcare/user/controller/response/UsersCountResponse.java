package it.pagopa.selfcare.user.controller.response;

import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.user.model.constants.OnboardedProductState;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UsersCountResponse {

    private String institutionId;
    private String productId;
    private List<PartyRole> roles;
    private List<OnboardedProductState> status;
    private Long count;

}
