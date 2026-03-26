package it.pagopa.selfcare.onboarding.core.strategy;

import java.util.List;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Data
@Service
public class UserAllowedValidationStrategyImpl implements UserAllowedValidationStrategy {

  private List<String> userAllowedList;

  @Autowired
  public UserAllowedValidationStrategyImpl(
      @Value("${user-allowed-list}") String userAllowedListKV) {

    if (userAllowedListKV.equalsIgnoreCase("null") || userAllowedListKV.trim().isEmpty()) {
      log.trace("Malformed string for user-allowed-list: {}", userAllowedListKV);
      userAllowedListKV = StringUtils.EMPTY;
    }

    log.trace("Initializing {}", UserAllowedValidationStrategyImpl.class.getSimpleName());
    log.debug(
        "UserAllowedValidationStrategyImpl = {}",
        userAllowedListKV.isEmpty() ? "Empty String for User Allowed" : userAllowedListKV);
    setUserAllowedList(validateUserString(userAllowedListKV));
  }

  public List<String> validateUserString(String userAllowedListKV) {
    List<String> userAllowed = List.of();
    if (StringUtils.isNotBlank(userAllowedListKV)) {
      userAllowed = List.of(userAllowedListKV.split(","));
      log.debug("User allowed {}", userAllowed.size());
    }
    return userAllowed;
  }

  @Override
  public boolean isAuthorizedUser(String uid) {
    boolean result = false;
    if (StringUtils.isNotBlank(uid)) {
      result = userAllowedList.contains(uid);
      log.debug("User with {} is Authorized", uid);
    }
    return result;
  }
}
