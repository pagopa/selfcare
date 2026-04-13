package it.pagopa.selfcare.onboarding.service;

import it.pagopa.selfcare.onboarding.client.model.onboarding.CheckManagerData;
import it.pagopa.selfcare.onboarding.client.model.onboarding.OnboardingData;
import it.pagopa.selfcare.onboarding.client.model.onboarding.User;
import it.pagopa.selfcare.onboarding.client.model.user.UserId;

public interface UserService {
  void validate(User user);

  void onboardingUsers(OnboardingData onboardingData);

  void onboardingUsersAggregator(OnboardingData onboardingData);

  boolean checkManager(CheckManagerData checkManagerData);

  User getManagerInfo(String onboardingId, String userTaxCode);

  boolean isAllowedUserByUid(String uid);

  UserId searchUser(String taxCode);
}
