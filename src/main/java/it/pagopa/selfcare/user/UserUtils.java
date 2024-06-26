package it.pagopa.selfcare.user;

import it.pagopa.selfcare.user.model.OnboardedProduct;
import it.pagopa.selfcare.user.model.TrackEventInput;
import it.pagopa.selfcare.user.model.constants.OnboardedProductState;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsLast;

public class UserUtils {

    /**
     * build an unique identifier for an event
     *
     * @param userInstitutionId
     * @param productId
     * @param productRole
     * @return
     */
    public static String uniqueIdNotification(String userInstitutionId, String productId, String productRole) {
        return String.format("%s_%s_%s", userInstitutionId, productId, productRole);
    }

    /**
     * Groups a list of OnboardedProduct instances by their productId and productRole,
     * then returns a collection of the products with the minimum status within each group.
     * Among products with the same minimum status, the one with the latest updatedAt
     * timestamp (and createdAt as a tiebreaker) is selected.
     * It is used when sending event occurs to send only the current valid role
     *
     * @param products the list of OnboardedProduct instances to be processed
     * @return a collection of OnboardedProduct instances with the minimum state for each productId and productRole group
     */
    public static Collection<OnboardedProduct> groupingProductAndReturnMinStateProduct(List<OnboardedProduct> products) {
        Map<String, List<OnboardedProduct>> mapProducts = products.stream()
                .collect(Collectors.groupingBy(product -> String.format("%s_%s", product.getProductId(), product.getProductRole())));

        Map<String, OnboardedProduct> onboardedProductMap = new HashMap<>();
        mapProducts
                .forEach((key, value) -> {
                    OnboardedProductState state = Collections.min(value.stream()
                            .map(OnboardedProduct::getStatus)
                            .toList());
                    value.stream()
                            .filter(product -> state.equals(product.getStatus()))
                            .max(Comparator.comparing(OnboardedProduct::getUpdatedAt, nullsLast(naturalOrder()))
                                    .thenComparing(OnboardedProduct::getCreatedAt, nullsLast(naturalOrder())))
                            .ifPresent(product -> onboardedProductMap.put(key, product));
                });


        return onboardedProductMap.values();
    }

    public static Map<String, String> mapPropsForTrackEvent(TrackEventInput trackEventInput) {
        Map<String, String> propertiesMap = new HashMap<>();
        Optional.ofNullable(trackEventInput.getDocumentKey()).ifPresent(value -> propertiesMap.put("documentKey", value));
        Optional.ofNullable(trackEventInput.getUserId()).ifPresent(value -> propertiesMap.put("userId", value));
        Optional.ofNullable(trackEventInput.getProductId()).ifPresent(value -> propertiesMap.put("productId", value));
        Optional.ofNullable(trackEventInput.getInstitutionId()).ifPresent(value -> propertiesMap.put("institutionId", value));
        Optional.ofNullable(trackEventInput.getProductRole()).ifPresent(value -> propertiesMap.put("productRole", value));
        Optional.ofNullable(trackEventInput.getException()).ifPresent(value -> propertiesMap.put("exec", value));
        return propertiesMap;
    }
}
