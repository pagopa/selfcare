package it.pagopa.selfcare.mscore.model.onboarding;

import it.pagopa.selfcare.mscore.model.user.UserToOnboard;
import lombok.Data;
import java.util.List;

@Data
public class OnboardingOperatorsRequest {

    private String productId;
    private String productTitle;
    private List<UserToOnboard> users;
    private String institutionId;

}
