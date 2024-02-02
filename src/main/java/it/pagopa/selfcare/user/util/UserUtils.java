package it.pagopa.selfcare.user.util;

import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.product.service.ProductService;
import it.pagopa.selfcare.user.constant.OnboardedProductState;
import it.pagopa.selfcare.user.constant.QueueEvent;
import it.pagopa.selfcare.user.entity.OnboardedProduct;
import it.pagopa.selfcare.user.entity.UserInfo;
import it.pagopa.selfcare.user.entity.UserInstitution;
import it.pagopa.selfcare.user.exception.InvalidRequestException;
import it.pagopa.selfcare.user.mapper.NotificationMapper;
import it.pagopa.selfcare.user.model.notification.UserNotificationToSend;
import it.pagopa.selfcare.user.model.notification.UserToNotify;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.gradle.internal.impldep.org.apache.commons.lang.StringUtils;
import org.jboss.resteasy.reactive.client.api.WebClientApplicationException;
import org.openapi.quarkus.user_registry_json.model.UserResource;

import java.util.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class UserUtils {

    private final ProductService productService;
    private final NotificationMapper notificationMapper;
    public static final List<String> VALID_USER_PRODUCT_STATES_FOR_NOTIFICATION = List.of(OnboardedProductState.ACTIVE.name(), OnboardedProductState.DELETED.name(), OnboardedProductState.SUSPENDED.name());

    @SafeVarargs
    public final Map<String, Object> retrieveMapForFilter(Map<String, Object>... maps) {
        Map<String, Object> map = new HashMap<>();
        Arrays.stream(maps).forEach(map::putAll);
        return map;
    }

    public void checkProductRole(String productId, PartyRole role, String productRole) {
        if(StringUtils.isNotBlank(productRole) && StringUtils.isNotBlank(productId)) {
            try {
                productService.validateProductRole(productId, productRole, role);
            }catch (IllegalArgumentException e){
                throw new InvalidRequestException(e.getMessage());
            }
        }
    }

    public static boolean checkIfNotFoundException(Throwable throwable) {
        if(throwable instanceof WebClientApplicationException wex) {
            return wex.getResponse().getStatus() == HttpStatus.SC_NOT_FOUND;
        }

        return false;
    }

    public List<UserNotificationToSend> buildUsersNotificationResponse(UserInstitution userInstitution, UserResource userResource, QueueEvent eventType) {
        return userInstitution.getProducts().stream()
                .map(onboardedProduct -> {
                    UserNotificationToSend userNotificationToSend = constructUserNotificationToSend(userInstitution, userResource, onboardedProduct);
                    userNotificationToSend.setId(idBuilder(userInstitution.getUserId(), userInstitution.getInstitutionId(), onboardedProduct.getProductId(), onboardedProduct.getProductRole()));
                    userNotificationToSend.setEventType(eventType);
                    return userNotificationToSend;
                })
                .toList();
    }

    private String idBuilder(String userId, String institutionId, String productId, String productRole){
        return String.format("%s_%s_%s_%s", userId, institutionId, productId, productRole);
    }

    public List<UserNotificationToSend> buildUsersNotificationResponse(UserInstitution userInstitution, UserResource userResource, String productId) {
        return userInstitution.getProducts().stream()
                .map(onboardedProduct -> {
                    if (StringUtils.isBlank(productId) ||  productId.equals(onboardedProduct.getProductId()) && VALID_USER_PRODUCT_STATES_FOR_NOTIFICATION.contains(onboardedProduct.getStatus().name())) {
                        return constructUserNotificationToSend(userInstitution, userResource, onboardedProduct);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .toList();
    }

    private UserNotificationToSend constructUserNotificationToSend(UserInstitution userInstitution, UserResource userResource, OnboardedProduct onboardedProduct) {
         UserToNotify userToNotify = notificationMapper.toUserNotify(userResource, onboardedProduct, userInstitution.getUserId());
         return notificationMapper.setNotificationDetailsFromOnboardedProduct(userToNotify, onboardedProduct, userInstitution.getInstitutionId());
    }

    /**
     * The filterProduct function takes in a UserInstitution object and an array of states.
     * It then creates a list of OnboardedProductState objects from the array of strings, if the array is not null.
     * If it is null, it sets relationshipStates to be null as well.
     * Then, for each product in userInstitution's products list:
     * if relationshipStates is not null and does not contain that product's status (which should be an OnboardedProductState),
     * remove that product from the list.
     */
    public UserInstitution filterProduct(UserInstitution userInstitution, String[] states) {
        List<OnboardedProductState> onboardedProductStates = Optional.ofNullable(states)
                .map(this::convertStatesToOnboardedProductStates)
                .orElse(null);

        if(Objects.nonNull(userInstitution.getProducts())) {
            userInstitution.getProducts().removeIf(onboardedProduct -> !Objects.isNull(onboardedProductStates) && !onboardedProductStates.contains(onboardedProduct.getStatus()));
        }
        return userInstitution;
    }

    public List<OnboardedProductState> convertStatesToOnboardedProductStates(String[] states) {
        return Arrays.stream(states)
                .map(OnboardedProductState::valueOf)
                .collect(Collectors.toList());
    }

    public UserInfo filterInstitutionRoles(UserInfo userInfo, String[] states, String institutionId) {
        List<OnboardedProductState> onboardedProductStates = Optional.ofNullable(states)
                .map(this::convertStatesToOnboardedProductStates)
                .orElse(null);

        if (Objects.nonNull(userInfo.getInstitutions())) {
            userInfo.getInstitutions().removeIf(institution -> (!Objects.isNull(onboardedProductStates) && !onboardedProductStates.contains(institution.getStatus()))
                    || (!Objects.isNull(institutionId) && !institution.getInstitutionId().equalsIgnoreCase(institutionId))
            );
        }
        return userInfo;
    }
}