package it.pagopa.selfcare.onboarding.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.client.model.OnboardingData;
import it.pagopa.selfcare.onboarding.client.model.User;
import it.pagopa.selfcare.onboarding.client.model.UserId;
import org.openapi.quarkus.onboarding_json.model.CheckManagerRequest;

public interface UserService {
  Uni<Void> validate(User user);

  Uni<Void> onboardingUsers(OnboardingData onboardingData);

  Uni<Void> onboardingUsersAggregator(OnboardingData onboardingData);

  Uni<Boolean> checkManager(CheckManagerRequest checkManagerData);

  Uni<User> getManagerInfo(String onboardingId, String userTaxCode);

  Uni<UserId> searchUser(String taxCode);
}
