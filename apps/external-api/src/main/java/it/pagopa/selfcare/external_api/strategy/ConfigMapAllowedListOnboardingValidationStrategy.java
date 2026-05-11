package it.pagopa.selfcare.external_api.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * It validates the onboarding request based on an allowed-list loaded from a property.
 */
@Slf4j
@Service
public class ConfigMapAllowedListOnboardingValidationStrategy implements OnboardingValidationStrategy {

    /**
     * It represents, if present, the institutions and products allowed to be onboarded (i.e. an allowed-list).
     * The {@code Map} has as key the product id  and as values a list of institution external id allowed for that product
     * A {@code *} value means "anything".
     * If used, the '*' is the only value allowed for a given key.
     */
    private final Optional<Map<String, Set<String>>> institutionProductsAllowedMap;


    @Autowired
    public ConfigMapAllowedListOnboardingValidationStrategy(@Value("#{${external_api.institutions-allowed-list}}") Map<String, Set<String>> institutionProductsAllowedMap) {
        log.trace("Initializing {}", ConfigMapAllowedListOnboardingValidationStrategy.class.getSimpleName());
        log.debug("ConfigMapAllowedListOnboardingValidationStrategy institutionProductsAllowedMap = {}", institutionProductsAllowedMap);
        validateSpecialcharecterUsage(institutionProductsAllowedMap);
        this.institutionProductsAllowedMap = Optional.ofNullable(institutionProductsAllowedMap);
    }


    private void validateSpecialcharecterUsage(Map<String, Set<String>> allowedList) {
        if (allowedList != null) {
            allowedList.forEach((productId, institutionExternalIds) -> {
                if (institutionExternalIds.size() > 1
                        && institutionExternalIds.stream().anyMatch("*"::equals)) {
                    throw new IllegalArgumentException(String.format("Invalid configuration: bad using of special character '*' in allowed-list for key '%s'. If used, the '*' is the only value allowed for a given key",
                            productId));
                }
            });
        }
    }


    /**
     * If the allowed-list is present and the provided {@code productId} and {@code institutionExternalId} are not in that list, an execption is thrown.
     * Otherwise, if the allowed-is is not present, then no validation is applied.
     *
     * @param productId             the product id
     * @param institutionExternalId the institution external id
     */
    @Override
    public boolean validate(String productId, String institutionExternalId) {
        log.trace("validate start");
        log.debug("validate productId = {}, institutionExternalId = {}", productId, institutionExternalId);
        final boolean valid = institutionProductsAllowedMap.isEmpty() ||
                Optional.ofNullable(institutionProductsAllowedMap.get().get(productId))
                        .map(institutionExternalIds -> institutionExternalIds.contains("*")
                                || institutionExternalIds.contains(institutionExternalId))
                        .orElse(false);
        log.debug("validate result = {}", valid);
        log.trace("validate end");
        return valid;
    }

}
