package it.pagopa.selfcare.onboarding.service.strategy;

import static org.junit.jupiter.api.Assertions.*;

import io.quarkus.test.junit.QuarkusTest;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

@QuarkusTest
class ConfigAllowedProductListOnboardingValidationStrategyTest {

  @Test
  void validate_allowedListNotConfigured() {
    // given
    String productId = "prod-io";

    ConfigAllowedProductListOnboardingValidationStrategy validationStrategy =
        new ConfigAllowedProductListOnboardingValidationStrategy(Optional.empty());

    // when
    Executable executable = () -> validationStrategy.validate(productId);

    // then
    assertDoesNotThrow(executable);
    assertTrue(validationStrategy.getInstitutionProductsAllowedList().get().isEmpty());
  }

  @Test
  void validate_productNotInConfig() {
    // given
    String productId = "prod-io";

    ConfigAllowedProductListOnboardingValidationStrategy validationStrategy =
        new ConfigAllowedProductListOnboardingValidationStrategy(Optional.of("prod-test"));

    // when
    boolean validate = validationStrategy.validate(productId);

    // then
    assertFalse(validate);
    assertEquals(1, validationStrategy.getInstitutionProductsAllowedList().get().size());
  }

  @Test
  void validate_allowedListSizeGreaterThanOne() {
    // given
    String productId = "prod-io";
    ConfigAllowedProductListOnboardingValidationStrategy validationStrategy =
        new ConfigAllowedProductListOnboardingValidationStrategy(
            Optional.of("prod-io,prod-test,prod-pagopa"));

    // when
    boolean validate = validationStrategy.validate(productId);

    // then
    assertTrue(validate);
    assertFalse(validationStrategy.getInstitutionProductsAllowedList().get().isEmpty());
    assertEquals(3, validationStrategy.getInstitutionProductsAllowedList().get().size());
  }
}
