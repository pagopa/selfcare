package it.pagopa.selfcare.onboarding.core.strategy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class UserAllowedValidationStrategyTest {

  UserAllowedValidationStrategyImpl userAllowedValidationStrategyImpl = null;

  @Test
  void validateUserStringTest() {
    // given
    System.setProperty("user-allowed-list", "uid1,uid2");
    String userString = System.getProperty("user-allowed-list");
    userAllowedValidationStrategyImpl = new UserAllowedValidationStrategyImpl(userString);

    // when
    List<String> result = userAllowedValidationStrategyImpl.validateUserString(userString);

    // then
    assertFalse(result.isEmpty());
    assertEquals(2, result.size());
  }

  @Test
  void validateUserStringTest_Case1() {
    // given
    System.setProperty("user-allowed-list", "uid1");
    String userString = System.getProperty("user-allowed-list");
    userAllowedValidationStrategyImpl = new UserAllowedValidationStrategyImpl(userString);

    // when
    List<String> result = userAllowedValidationStrategyImpl.validateUserString(userString);

    // then
    assertFalse(result.isEmpty());
    assertEquals(1, result.size());
  }

  @Test
  void validateUserStringTest_EmptyString() {
    // given
    System.setProperty("user-allowed-list", "");
    String userString = System.getProperty("user-allowed-list");
    userAllowedValidationStrategyImpl = new UserAllowedValidationStrategyImpl(userString);

    // when
    List<String> result = userAllowedValidationStrategyImpl.validateUserString(userString);

    // then
    assertTrue(result.isEmpty());
  }

  @Test
  void isAuthorizedTest() {
    // given
    System.setProperty("user-allowed-list", "uid1");
    String userString = System.getProperty("user-allowed-list");

    userAllowedValidationStrategyImpl = new UserAllowedValidationStrategyImpl(userString);

    // when
    boolean result = userAllowedValidationStrategyImpl.isAuthorizedUser("uid1");

    // then
    assertTrue(result);
  }

  @Test
  void isAuthorizedTest_CaseNullString() {
    // given
    System.setProperty("user-allowed-list", "null");
    String userString = System.getProperty("user-allowed-list");

    userAllowedValidationStrategyImpl = new UserAllowedValidationStrategyImpl(userString);

    // when
    boolean result = userAllowedValidationStrategyImpl.isAuthorizedUser(StringUtils.EMPTY);

    // then
    assertFalse(result);
  }

  @Test
  void isAuthorizedTest_CaseEmptyInput() {
    // given
    System.setProperty("user-allowed-list", "uid1");
    String userString = System.getProperty("user-allowed-list");

    userAllowedValidationStrategyImpl = new UserAllowedValidationStrategyImpl(userString);

    // when
    boolean result = userAllowedValidationStrategyImpl.isAuthorizedUser(StringUtils.EMPTY);

    // then
    assertFalse(result);
  }

  @AfterEach
  void cleanup() {
    System.clearProperty("user-allowed-list");
  }
}
