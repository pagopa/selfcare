package it.pagopa.selfcare.user.util;

import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.product.service.ProductService;
import it.pagopa.selfcare.user.constant.OnboardedProductState;
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

import java.util.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

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

    public List<UserNotificationToSend> constructUserNotificationToSend(UserInstitution userInstitution, org.openapi.quarkus.user_registry_json.model.UserResource userResource, String productId) {
        return userInstitution.getProducts().stream()
                .map(onboardedProduct -> {
                    if (StringUtils.isBlank(productId) ||  productId.equals(onboardedProduct.getProductId()) && VALID_USER_PRODUCT_STATES_FOR_NOTIFICATION.contains(onboardedProduct.getStatus().name())) {
                        UserToNotify userToNotify = notificationMapper.toUserNotify(userResource, onboardedProduct, userInstitution.getUserId());
                        return notificationMapper.setNotificationDetailsFromOnboardedProduct(userToNotify, onboardedProduct, userInstitution.getInstitutionId());
                    }
                    return null;
                })
                .toList();
    }

}