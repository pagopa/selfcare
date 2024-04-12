package it.pagopa.selfcare.user.util;

import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.smallrye.jwt.auth.principal.DefaultJWTCallerPrincipal;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.product.service.ProductService;
import it.pagopa.selfcare.user.constant.OnboardedProductState;
import it.pagopa.selfcare.user.constant.QueueEvent;
import it.pagopa.selfcare.user.entity.OnboardedProduct;
import it.pagopa.selfcare.user.entity.UserInfo;
import it.pagopa.selfcare.user.entity.UserInstitution;
import it.pagopa.selfcare.user.exception.InvalidRequestException;
import it.pagopa.selfcare.user.mapper.NotificationMapper;
import it.pagopa.selfcare.user.model.LoggedUser;
import it.pagopa.selfcare.user.model.notification.UserNotificationToSend;
import it.pagopa.selfcare.user.model.notification.UserToNotify;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.SecurityContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.gradle.internal.impldep.org.apache.commons.lang.StringUtils;
import org.jboss.resteasy.reactive.ClientWebApplicationException;
import org.jboss.resteasy.reactive.client.api.WebClientApplicationException;
import org.openapi.quarkus.user_registry_json.model.CertifiableFieldResourceOfstring;
import org.openapi.quarkus.user_registry_json.model.UserResource;
import org.openapi.quarkus.user_registry_json.model.WorkContactResource;

import java.util.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static it.pagopa.selfcare.user.constant.CollectionUtil.MAIL_ID_PREFIX;

@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class UserUtils {

    @Inject
    CurrentIdentityAssociation currentIdentityAssociation;

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
        if (StringUtils.isNotBlank(productRole) && StringUtils.isNotBlank(productId)) {
            try {
                productService.validateProductRole(productId, productRole, role);
            } catch (IllegalArgumentException e) {
                throw new InvalidRequestException(e.getMessage());
            }
        }
    }

    public static boolean checkIfNotFoundException(Throwable throwable) {
        if (throwable instanceof ClientWebApplicationException wex) {
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

    public UserNotificationToSend buildUserNotificationToSend(UserInstitution userInstitution, UserResource userResource, String productId, OnboardedProductState status) {
        UserToNotify userToNotify = buildUserToNotify(userResource, userInstitution, status);
        UserNotificationToSend userNotificationToSend = buildUserNotificationToSend(userInstitution, productId);

        userInstitution.getProducts().stream()
                .filter(p -> org.apache.commons.lang3.StringUtils.equals(p.getProductId(), productId))
                .findFirst()
                .ifPresent(onboardedProductResponse -> {
                    userToNotify.setRole(onboardedProductResponse.getRole());
                    userToNotify.setProductRole(onboardedProductResponse.getProductRole());

                    userNotificationToSend.setUser(userToNotify);
                    userNotificationToSend.setOnboardingTokenId(onboardedProductResponse.getTokenId());
                    userNotificationToSend.setCreatedAt(onboardedProductResponse.getCreatedAt());
                    userNotificationToSend.setUpdatedAt(onboardedProductResponse.getUpdatedAt());
                });

        return userNotificationToSend;
    }

    private UserNotificationToSend buildUserNotificationToSend(UserInstitution institution, String productId) {
        UserNotificationToSend userNotificationToSend = new UserNotificationToSend();
        userNotificationToSend.setId(institution.getUserId());
        userNotificationToSend.setInstitutionId(institution.getInstitutionId());
        userNotificationToSend.setProductId(productId);
        userNotificationToSend.setEventType(QueueEvent.UPDATE);
        return userNotificationToSend;

    }

    private UserToNotify buildUserToNotify(UserResource user, UserInstitution institution, OnboardedProductState status) {
        UserToNotify userToNotify = new UserToNotify();
        userToNotify.setUserId(institution.getUserId());
        userToNotify.setName(Optional.ofNullable(user.getName()).map(CertifiableFieldResourceOfstring::getValue).orElse(null));
        userToNotify.setFamilyName(Optional.ofNullable(user.getFamilyName()).map(CertifiableFieldResourceOfstring::getValue).orElse(null));
        userToNotify.setEmail(Optional.ofNullable(user.getEmail()).map(CertifiableFieldResourceOfstring::getValue).orElse(null));
        userToNotify.setRelationshipStatus(status);
        return userToNotify;

    }

    private String idBuilder(String userId, String institutionId, String productId, String productRole) {
        return String.format("%s_%s_%s_%s", userId, institutionId, productId, productRole);
    }

    public List<UserNotificationToSend> buildUsersNotificationResponse(UserInstitution userInstitution, UserResource userResource, String productId) {
        return userInstitution.getProducts().stream()
                .filter(Objects::nonNull)
                .map(onboardedProduct -> {
                    if (StringUtils.isBlank(productId) || productId.equals(onboardedProduct.getProductId()) && VALID_USER_PRODUCT_STATES_FOR_NOTIFICATION.contains(onboardedProduct.getStatus().name())) {
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

        userInstitution.getProducts().removeIf(onboardedProduct -> !Objects.isNull(onboardedProductStates) && !onboardedProductStates.contains(onboardedProduct.getStatus()));

        return userInstitution;
    }

    public List<OnboardedProductState> convertStatesToOnboardedProductStates(String[] states) {
        return Arrays.stream(states)
                .map(OnboardedProductState::valueOf)
                .toList();
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

    public static WorkContactResource buildWorkContact(String mail) {
        return WorkContactResource.builder()
                .email(new CertifiableFieldResourceOfstring(
                        CertifiableFieldResourceOfstring.CertificationEnum.NONE,
                        mail)
                ).build();
    }

    public static boolean isUserNotFoundExceptionOnUserRegistry(Throwable fail) {
        return fail instanceof WebApplicationException webApplicationException && webApplicationException.getResponse().getStatus() == HttpStatus.SC_NOT_FOUND;
    }

    public Optional<String> getMailUuidFromMail(Map<String, WorkContactResource> workContacts, String email) {
        return workContacts.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(MAIL_ID_PREFIX) && entry.getValue().getEmail() != null
                        && org.apache.commons.lang3.StringUtils.equals(entry.getValue().getEmail().getValue(), email))
                .map(Map.Entry::getKey)
                .findFirst();
    }

    public Uni<LoggedUser> readUserIdFromToken(SecurityContext ctx) {
        return currentIdentityAssociation.getDeferredIdentity()
                .onItem().transformToUni(identity -> {
                    if (ctx.getUserPrincipal() == null || !ctx.getUserPrincipal().getName().equals(identity.getPrincipal().getName())) {
                        return Uni.createFrom().failure(new InternalServerErrorException("Principal and JsonWebToken names do not match"));
                    }

                    if (identity.getPrincipal() instanceof DefaultJWTCallerPrincipal jwtCallerPrincipal) {
                        String uid = jwtCallerPrincipal.getClaim("uid");
                        String familyName = jwtCallerPrincipal.getClaim("family_name");
                        String name = jwtCallerPrincipal.getClaim("name");
                        return Uni.createFrom().item(
                                LoggedUser.builder()
                                        .uid(uid)
                                        .familyName(familyName)
                                        .name(name)
                                        .build()
                        );
                    }
                    return Uni.createFrom().nullItem();
                });
    }

}