package it.pagopa.selfcare.onboarding.service.strategy;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/** It validate the onboarding request based on an allowed-list loaded from a property. */
@Slf4j
@Data
@ApplicationScoped
public class ConfigAllowedProductListOnboardingValidationStrategy implements OnboardingValidationStrategy {

  /**
   * Constructor for {@code ConfigAllowedProductListOnboardingValidationStrategy}. Initializes the
   * validation strategy based on a list of allowed products, obtained from an external
   * configuration.
   *
   * <p>The method reads a configuration string representing a list of allowed products for
   * onboarding institutions. If the string is present and is not "null" (ignoring case), the string
   * is validated and converted to a list of {@code String}. Otherwise, an empty list is used.
   * IllegalArgumentException If the configuration string contains invalid products.
   *
   * @see #validateProductString(String)
   * @see #setInstitutionProductsAllowedList(Optional)
   */
  private Optional<List<String>> institutionProductsAllowedList;

  public ConfigAllowedProductListOnboardingValidationStrategy(
      @ConfigProperty(name = "onboarding.institutions-allowed-list")
          Optional<String> institutionProductsAllowedString) {
    log.trace(
        "Initializing {}", ConfigAllowedProductListOnboardingValidationStrategy.class.getSimpleName());
    log.debug(
        "ConfigAllowedProductListOnboardingValidationStrategy institutionProductsAllowedList = {}",
        institutionProductsAllowedString);
    List<String> institutionProductsAllowed = List.of();

    if (institutionProductsAllowedString.isPresent()
        && !"null".equalsIgnoreCase(institutionProductsAllowedString.get())) {
      institutionProductsAllowed = validateProductString(institutionProductsAllowedString.get());
    }

    log.debug("institutionProductsAllowedList size= {}", institutionProductsAllowed.size());
    setInstitutionProductsAllowedList(Optional.of(institutionProductsAllowed));
    log.trace(
        "Ending config for {}",
        ConfigAllowedProductListOnboardingValidationStrategy.class.getSimpleName());
  }

  private List<String> validateProductString(String institutionProductsAllowedString) {
    List<String> productAllowed = List.of();
    if (StringUtils.isNotBlank(institutionProductsAllowedString)) {
      productAllowed = List.of(institutionProductsAllowedString.split(","));
      log.debug("Product allowed {}", productAllowed.size());
    }
    return productAllowed;
  }

  /**
   * Validates the provided {@code productId} against the allowed list of product IDs.
   *
   * <p>If an allowed list is configured, this method checks if the provided {@code productId}
   * exists within that list. If the {@code productId} is found, the method returns {@code true};
   * otherwise, it returns {@code false}.
   *
   * <p>If no allowed list is configured (i.e., the allowed list is absent or empty), this method
   * bypasses validation and returns {@code true}, effectively allowing any {@code productId}.
   *
   * @param productId The product ID to validate.
   * @return {@code true} if the {@code productId} is valid (either found in the allowed list or no
   *     allowed list is configured), {@code false} otherwise.
   */
  @Override
  public boolean validate(String productId) {
    log.trace("Validate by productId start");
    log.debug("Provided productId = {}", productId);
    boolean result =
        institutionProductsAllowedList.map(product -> product.contains(productId)).orElse(false);
    log.debug("Validate result = {}", result);
    log.trace("Validate end");
    return result;
  }
}
