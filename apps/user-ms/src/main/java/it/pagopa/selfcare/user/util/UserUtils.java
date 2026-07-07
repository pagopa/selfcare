package it.pagopa.selfcare.user.util;

import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.smallrye.jwt.auth.principal.DefaultJWTCallerPrincipal;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.product.entity.ProductRole;
import it.pagopa.selfcare.product.entity.ProductRoleInfo;
import it.pagopa.selfcare.product.service.ProductService;
import it.pagopa.selfcare.user.entity.UserInstitution;
import it.pagopa.selfcare.user.exception.InvalidRequestException;
import it.pagopa.selfcare.user.mapper.NotificationMapper;
import it.pagopa.selfcare.user.model.LoggedUser;
import it.pagopa.selfcare.user.model.OnboardedProduct;
import it.pagopa.selfcare.user.model.UserNotificationToSend;
import it.pagopa.selfcare.user.model.constants.OnboardedProductState;
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
import org.openapi.quarkus.user_registry_json.model.*;

import java.util.*;
import java.util.stream.Collectors;

import static it.pagopa.selfcare.user.constant.CollectionUtil.MAIL_ID_PREFIX;
import static it.pagopa.selfcare.user.model.constants.OnboardedProductState.ACTIVE;
import static it.pagopa.selfcare.user.model.constants.OnboardedProductState.SUSPENDED;

@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class UserUtils {

    @Inject
    CurrentIdentityAssociation currentIdentityAssociation;

    private final ProductService productService;
    private final NotificationMapper notificationMapper;
    public static final List<String> VALID_USER_PRODUCT_STATES_FOR_NOTIFICATION = List.of(ACTIVE.name(), OnboardedProductState.DELETED.name(), SUSPENDED.name());

    @SafeVarargs
    public final Map<String, Object> retrieveMapForFilter(Map<String, Object>... maps) {
        Map<String, Object> map = new HashMap<>();
        Arrays.stream(maps).forEach(map::putAll);
        return map;
    }

    public Uni<Void> checkProductRolesAndValidateRequestMultirole(String productId, PartyRole role, List<String> productRoles) {

        if (StringUtils.isBlank(productId) || productRoles == null || productRoles.isEmpty()) {
          return Uni.createFrom().voidItem();
        }

        try {

          List<OnboardedProduct> finalState = productRoles.stream()
            .map(productRole -> {

              // Validate that the productRole belongs to the requested PartyRole
              productService.validateProductRole(productId, productRole, role);

              //simulate the addition of new roles
              OnboardedProduct onboardedProduct = new OnboardedProduct();
              onboardedProduct.setProductId(productId);
              onboardedProduct.setRole(role);
              onboardedProduct.setProductRole(productRole);
              onboardedProduct.setStatus(ACTIVE);

              return onboardedProduct;
            })
            .toList();

          Map<PartyRole, ProductRoleInfo> roleMappings =
            productService.getProduct(productId).getRoleMappings();

          // validate multirole addition
          validateMultiroleConfiguration(finalState, roleMappings);

        } catch (IllegalArgumentException e) {
          throw new InvalidRequestException(e.getMessage());
        }

        return Uni.createFrom().voidItem();
    }

    public Uni<Void> validateMultiroleWithUserInstitution(String productId, String partyRole, List<String> productRoles, UserInstitution userInstitution) {

        if (userInstitution == null || StringUtils.isBlank(productId) || productRoles == null || productRoles.isEmpty()) {
          return Uni.createFrom().voidItem();
        }

        List<OnboardedProduct> finalState = new ArrayList<>(
          userInstitution.getProducts().stream()
            .filter(p -> productId.equals(p.getProductId()))
            .filter(p -> p.getStatus() == ACTIVE || p.getStatus() == SUSPENDED)
            .toList());

        // no existing role -> no check needed
        if (finalState.isEmpty()) {
          return Uni.createFrom().voidItem();
        }

        Map<PartyRole, ProductRoleInfo> roleMappings =
          productService.getProduct(productId).getRoleMappings();

        ProductRoleInfo requestedRoleInfo =
          roleMappings.get(PartyRole.valueOf(partyRole));

        if (requestedRoleInfo == null) {
          throw new InvalidRequestException(
            "No role mapping for partyRole " + partyRole);
        }

        // simulate the addition of new roles
        requestedRoleInfo.getRoles().stream()
          .filter(role -> productRoles.contains(role.getCode()))
          .forEach(role -> {
            OnboardedProduct onboardedProduct = new OnboardedProduct();
            onboardedProduct.setProductId(productId);
            onboardedProduct.setRole(PartyRole.valueOf(partyRole));
            onboardedProduct.setProductRole(role.getCode());
            onboardedProduct.setStatus(ACTIVE);

            finalState.add(onboardedProduct);
          });

        validateMultiroleConfiguration(finalState, roleMappings);

        return Uni.createFrom().voidItem();
    }

  public Uni<Void> validateMultiroleAfterStatusUpdate(List<UserInstitution> userInstitutions, String productId, PartyRole targetRole, String targetProductRole, OnboardedProductState newStatus) {

    if(newStatus != ACTIVE && newStatus != SUSPENDED) {
      return Uni.createFrom().voidItem();
    }

    Map<String, Map<PartyRole, ProductRoleInfo>> roleMappingsByProduct =
      new HashMap<>();


    for (UserInstitution userInstitution : userInstitutions) {

      List<OnboardedProduct> filteredProducts = userInstitution.getProducts().stream()
        .filter(p -> productId == null || productId.equals(p.getProductId()))
        .toList();

      // GROUP ONBOARDEDPRODUCTS BY PRODUCT
      Map<String, List<OnboardedProduct>> byProduct = filteredProducts.stream()
        .collect(Collectors.groupingBy(OnboardedProduct::getProductId));

      for (Map.Entry<String, List<OnboardedProduct>> entry : byProduct.entrySet()) {

        String currentProductId = entry.getKey();
        List<OnboardedProduct> products = entry.getValue();

        Map<PartyRole, ProductRoleInfo> roleMappings =
          roleMappingsByProduct.computeIfAbsent(
            currentProductId,
            pid -> productService.getProduct(pid).getRoleMappings()
          );

        // SIMULATE FINAL STATE
        List<OnboardedProduct> finalState = products.stream()
          .map(p -> simulateUpdate(p, productId, targetRole, targetProductRole, newStatus))
          .filter(p -> p.getStatus() == ACTIVE || p.getStatus() == SUSPENDED)
          .toList();

        //VALIDATION
        validateMultiroleConfiguration(finalState, roleMappings);
      }
    }
    return Uni.createFrom().voidItem();
  }


    private OnboardedProduct simulateUpdate(OnboardedProduct p, String productId, PartyRole targetRole, String targetProductRole, OnboardedProductState newStatus) {

        OnboardedProduct onboardedProductCopy = new OnboardedProduct();
        onboardedProductCopy.setProductId(p.getProductId());
        onboardedProductCopy.setRole(p.getRole());
        onboardedProductCopy.setProductRole(p.getProductRole());
        onboardedProductCopy.setStatus(p.getStatus());

        boolean matches = true;

        if (productId != null) {
          matches &= productId.equals(onboardedProductCopy.getProductId());
        }

        if (targetRole != null) {
          matches &= targetRole.equals(onboardedProductCopy.getRole());
        }

        if (targetProductRole != null) {
          matches &= targetProductRole.equals(onboardedProductCopy.getProductRole());
        }

        if (matches) {
          onboardedProductCopy.setStatus(newStatus);
        }

        return onboardedProductCopy;
    }

    private void validateMultiroleConfiguration(List<OnboardedProduct> finalState, Map<PartyRole, ProductRoleInfo> roleMappings) {

        //if it has no active or suspended roles or if it has only one role, no need to check multirole rules
        if (finalState.size() <= 1) {
          return;
        }

        // COMMON GROUPS CHECK: if there's an intersection between the multirole groups of all the roles, the configuration is valid
        Set<String> commonGroups = finalState.stream()
          .map(p -> {
            ProductRoleInfo info = roleMappings.get(p.getRole());
            if (info == null) {
              throw new InvalidRequestException(
                "No role mapping for partyRole " + p.getRole());
            }
            return info.getRoles().stream()
            .filter(r -> r.getCode().equals(p.getProductRole()))
            .findFirst()
            .map(ProductRole::getMultiroleGroups)
            .map(groups -> (Set<String>) new HashSet<>(groups))
            .orElse(Collections.emptySet());
          })
          .reduce((g1, g2) -> {
            Set<String> inter = new HashSet<>(g1);
            inter.retainAll(g2);
            return inter;
          })
          .orElse(Collections.emptySet());

        if (commonGroups.isEmpty()) {
          throw new InvalidRequestException(
            "Not valid multirole configuration");
        }
    }

    public static boolean checkIfNotFoundException(Throwable throwable) {
        if (throwable instanceof ClientWebApplicationException wex) {
            return wex.getResponse().getStatus() == HttpStatus.SC_NOT_FOUND;
        }

        return false;
    }

    public List<UserNotificationToSend> buildUsersNotificationResponse(UserInstitution userInstitution, UserResource userResource) {
        /*
         * Since this service is used to re-send events from scratch we must send all "history" without grouping by product and state
         */
        return userInstitution.getProducts()
                .stream()
                .map(onboardedProduct ->  notificationMapper.toUserNotificationToSend(userInstitution, onboardedProduct, userResource))
                .toList();
    }


    public List<UserNotificationToSend> buildUsersNotificationResponse(UserInstitution userInstitution, UserResource userResource, String productId) {
        return userInstitution.getProducts().stream()
                .filter(Objects::nonNull)
                .map(onboardedProduct -> {
                    if (StringUtils.isBlank(productId) || productId.equals(onboardedProduct.getProductId()) && VALID_USER_PRODUCT_STATES_FOR_NOTIFICATION.contains(onboardedProduct.getStatus().name())) {
                        return notificationMapper.toUserNotificationToSend(userInstitution, onboardedProduct, userResource);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .toList();
    }

    public static WorkContactResource buildWorkContact(String mail) {
        return WorkContactResource.builder()
                .email(new EmailCertifiableSchema(
                        EmailCertifiableSchema.CertificationEnum.NONE,
                        mail)
                ).build();
    }

    public static boolean isUserNotFoundExceptionOnUserRegistry(Throwable fail) {
        return fail instanceof WebApplicationException webApplicationException && webApplicationException.getResponse().getStatus() == HttpStatus.SC_NOT_FOUND;
    }

    public static boolean isConflictOnUserRegistry(Throwable fail) {
        return fail instanceof WebApplicationException webApplicationException && webApplicationException.getResponse().getStatus() == HttpStatus.SC_CONFLICT;
    }

    public Optional<String> getMailUuidFromMail(Map<String, WorkContactResource> workContacts, String email) {
        if(Objects.isNull(workContacts) || workContacts.isEmpty()) return Optional.empty();

        return workContacts.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(MAIL_ID_PREFIX) && entry.getValue().getEmail() != null
                        && org.apache.commons.lang3.StringUtils.equals(entry.getValue().getEmail().getValue(), email))
                .map(Map.Entry::getKey)
                .findFirst();
    }

    public static Optional<String> getMailByMailUuid(Map<String, WorkContactResource> workContacts, String mailUid) {
        if(Objects.isNull(workContacts) || workContacts.isEmpty() || Objects.isNull(mailUid)) return Optional.empty();

        return workContacts.entrySet().stream()
                .filter(entry -> entry.getKey().equals(mailUid))
                .map(Map.Entry::getValue)
                .filter(Objects::nonNull)
                .map(WorkContactResource::getEmail)
                .filter(Objects::nonNull)
                .map(EmailCertifiableSchema::getValue)
                .filter(Objects::nonNull)
                .findFirst();
    }

    public static Optional<String> getMobilePhoneByMailUuid(Map<String, WorkContactResource> workContacts, String mailUid) {
        if(Objects.isNull(workContacts) || workContacts.isEmpty() || Objects.isNull(mailUid)) return Optional.empty();

        return workContacts.entrySet().stream()
                .filter(entry -> entry.getKey().equals(mailUid))
                .map(Map.Entry::getValue)
                .filter(Objects::nonNull)
                .map(WorkContactResource::getMobilePhone)
                .filter(Objects::nonNull)
                .map(MobilePhoneCertifiableSchema::getValue)
                .filter(Objects::nonNull)
                .findFirst();
    }

    public static Optional<String> getTelephoneByMailUuid(Map<String, WorkContactResource> workContacts, String mailUid) {
        if(Objects.isNull(workContacts) || workContacts.isEmpty() || Objects.isNull(mailUid)) return Optional.empty();

        return workContacts.entrySet().stream()
                .filter(entry -> entry.getKey().equals(mailUid))
                .map(Map.Entry::getValue)
                .filter(Objects::nonNull)
                .map(WorkContactResource::getTelephone)
                .filter(Objects::nonNull)
                .map(TelephoneCertifiableSchema::getValue)
                .filter(Objects::nonNull)
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
