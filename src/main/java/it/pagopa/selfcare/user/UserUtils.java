package it.pagopa.selfcare.user;

import it.pagopa.selfcare.user.model.OnboardedProduct;
import it.pagopa.selfcare.user.model.TrackEventInput;
import it.pagopa.selfcare.user.model.constants.OnboardedProductState;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsLast;

@Slf4j
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

    /**
     * Groups a list of OnboardedProduct instances by their productId and Role,
     * then returns a collection of the products with the minimum status within each group.
     * Among products with the same minimum status, the one with the latest updatedAt
     * timestamp (and createdAt as a tiebreaker) is selected.
     * It is used when sending event occurs to send only the current valid role
     *
     * @param products the list of OnboardedProduct instances to be processed
     * @return a collection of OnboardedProduct instances with the minimum state for each productId and PartyRole group
     */
    public static Collection<OnboardedProduct> groupingProductWithRoleAndReturnMinStateProduct(List<OnboardedProduct> products) {
        Map<String, List<OnboardedProduct>> mapProducts = products.stream()
                .collect(Collectors.groupingBy(product -> String.format("%s_%s", product.getProductId(), product.getRole())));

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
        Optional.ofNullable(trackEventInput.getGroupMembers()).ifPresent(value -> propertiesMap.put("groupMembers", String.join(",", value)));
        Optional.ofNullable(trackEventInput.getException()).ifPresent(value -> propertiesMap.put("exec", value));
        return propertiesMap;
    }

    /**
     * The retrieveFdProductIfItChanged method is designed to retrieve the most recently updated OnboardedProduct
     * from a list of products, provided that the product's ID is included in a specified list of product IDs to check.
     */
    public static List<OnboardedProduct> retrieveFdProduct(List<OnboardedProduct> products, List<String> productIdToCheck, boolean isUserMailChanged) {
        if (Objects.nonNull(products) && !products.isEmpty()) {
            if(isUserMailChanged){
                return products.stream()
                        .filter(onboardedProduct -> productIdToCheck.contains(onboardedProduct.getProductId()) && OnboardedProductState.ACTIVE.equals(onboardedProduct.getStatus()))
                        .toList();
            }
            OnboardedProduct onboardedProduct = products.stream()
                    .max(Comparator.comparing(OnboardedProduct::getUpdatedAt, nullsLast(naturalOrder()))
                            .thenComparing(OnboardedProduct::getCreatedAt, nullsLast(naturalOrder())))
                    .filter(prod -> productIdToCheck.contains(prod.getProductId()))
                    .orElse(null);

            if(Objects.nonNull(onboardedProduct)) {
                return List.of(onboardedProduct);
            }
        }
        return Collections.emptyList();
    }

    public static String getSASToken(String resourceUri, String keyName, String key) {
        long epoch = System.currentTimeMillis() / 1000L;
        int week = 60 * 60 * 24 * 7;
        String expiry = Long.toString(epoch + week);

        String sasToken;
        String stringToSign = URLEncoder.encode(resourceUri, StandardCharsets.UTF_8) + "\n" + expiry;
        String signature = getHMAC256(key, stringToSign);
        sasToken = "SharedAccessSignature sr=" + URLEncoder.encode(resourceUri, StandardCharsets.UTF_8) + "&sig=" +
                URLEncoder.encode(signature, StandardCharsets.UTF_8) + "&se=" + expiry + "&skn=" + keyName;
        return sasToken;
    }


    public static String getHMAC256(String key, String input) {
        Mac sha256HMAC;
        String hash = null;
        try {
            sha256HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), "HmacSHA256");
            sha256HMAC.init(secretKey);
            Base64.Encoder encoder = Base64.getEncoder();

            hash = new String(encoder.encode(sha256HMAC.doFinal(input.getBytes(StandardCharsets.UTF_8))));

        } catch (InvalidKeyException | NoSuchAlgorithmException | IllegalStateException e) {
            log.error("Exception: {}", e.getMessage(), e);
        }

        return hash;
    }
}
