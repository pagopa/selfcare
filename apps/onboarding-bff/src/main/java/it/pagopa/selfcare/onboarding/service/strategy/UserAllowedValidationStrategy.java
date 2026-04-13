package it.pagopa.selfcare.onboarding.service.strategy;

import java.util.List;

public interface UserAllowedValidationStrategy {

  List<String> validateUserString(String userAllowedListKV);

  boolean isAuthorizedUser(String uid);
}
