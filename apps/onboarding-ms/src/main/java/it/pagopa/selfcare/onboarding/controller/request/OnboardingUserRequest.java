package it.pagopa.selfcare.onboarding.controller.request;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class OnboardingUserRequest extends OnboardingUsersBaseRequest {

    private Boolean toAddOnAggregates;
}
