package it.pagopa.selfcare.external_api.model.onboarding;

import it.pagopa.selfcare.external_api.model.user.UserToOnboard;
import lombok.Data;

import java.util.List;
@Data
public class OnboardingUsersRequest {

    private String productId;
    private List<UserToOnboard> users;
    private String institutionId;
    private String institutionTaxCode;
    private String institutionSubunitCode;
    private Boolean sendCreateUserNotificationEmail;
    private Boolean toAddOnAggregates;

}
