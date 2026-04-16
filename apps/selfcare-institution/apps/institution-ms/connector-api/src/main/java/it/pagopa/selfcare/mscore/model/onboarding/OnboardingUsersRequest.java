package it.pagopa.selfcare.mscore.model.onboarding;

import it.pagopa.selfcare.mscore.model.user.UserToOnboard;
import lombok.Data;

import java.util.List;

@Data
public class OnboardingUsersRequest {

    private String productId;
    private List<UserToOnboard> users;
    private String institutionTaxCode;
    private String institutionSubunitCode;
    private Boolean sendCreateUserNotificationEmail;

}
