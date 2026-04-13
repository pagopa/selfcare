package it.pagopa.selfcare.onboarding.service;

import it.pagopa.selfcare.onboarding.client.model.OnboardingData;
import it.pagopa.selfcare.onboarding.client.model.User;
import it.pagopa.selfcare.onboarding.client.model.UserId;
import org.openapi.quarkus.onboarding_json.model.CheckManagerRequest;

public interface UserService {
  void validate(User user);

  void onboardingUsers(OnboardingData onboardingData);

  void onboardingUsersAggregator(OnboardingData onboardingData);

  boolean checkManager(CheckManagerRequest checkManagerData);

  User getManagerInfo(String onboardingId, String userTaxCode);

  boolean isAllowedUserByUid(String uid);

  UserId searchUser(String taxCode);
}
