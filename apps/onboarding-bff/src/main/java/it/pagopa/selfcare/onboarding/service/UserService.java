package it.pagopa.selfcare.onboarding.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.client.model.OnboardingData;
import it.pagopa.selfcare.onboarding.client.model.User;
import it.pagopa.selfcare.onboarding.client.model.UserId;
import org.openapi.quarkus.onboarding_json.model.CheckManagerRequest;

public interface UserService {
  void validate(User user);

  void onboardingUsers(OnboardingData onboardingData);

  void onboardingUsersAggregator(OnboardingData onboardingData);

  Uni<Boolean> checkManager(CheckManagerRequest checkManagerData);

  User getManagerInfo(String onboardingId, String userTaxCode);

  UserId searchUser(String taxCode);
}
