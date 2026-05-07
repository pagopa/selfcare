package it.pagopa.selfcare.mscore.model.onboarding;

import it.pagopa.selfcare.mscore.model.user.UserToOnboard;
import it.pagopa.selfcare.mscore.constant.TokenType;
import lombok.Data;
import java.util.List;

@Data
public class OnboardingLegalsRequest {

    private TokenType tokenType;
    private String productId;
    private String productName;
    private List<UserToOnboard> users;
    private String institutionExternalId;
    private String institutionId;
    private Contract contract;
    private boolean signContract;
}
