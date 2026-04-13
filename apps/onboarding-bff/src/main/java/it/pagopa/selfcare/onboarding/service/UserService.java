package it.pagopa.selfcare.onboarding.service;

import it.pagopa.selfcare.onboarding.client.model.CheckManagerData;
import it.pagopa.selfcare.onboarding.client.model.OnboardingData;
import it.pagopa.selfcare.onboarding.client.model.User;
import it.pagopa.selfcare.onboarding.client.model.UserId;

public interface UserService {
  void validate(User user);

  void onboardingUsers(OnboardingData onboardingData);

  void onboardingUsersAggregator(OnboardingData onboardingData);

  boolean checkManager(CheckManagerData checkManagerData);

  User getManagerInfo(String onboardingId, String userTaxCode);

  boolean isAllowedUserByUid(String uid);

  UserId searchUser(String taxCode);
}
