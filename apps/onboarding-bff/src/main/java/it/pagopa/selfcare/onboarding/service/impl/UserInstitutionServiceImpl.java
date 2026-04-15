package it.pagopa.selfcare.onboarding.service.impl;

import it.pagopa.selfcare.onboarding.service.*;

import it.pagopa.selfcare.onboarding.client.model.UserInstitutionRequest;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.product.entity.ProductStatus;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.user_json.api.InstitutionControllerApi;

import java.util.List;
import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.EMPTY;

@Slf4j
@ApplicationScoped
public class UserInstitutionServiceImpl implements UserInstitutionService {

  private final InstitutionControllerApi userInstitutionApi;

  public UserInstitutionServiceImpl(@RestClient InstitutionControllerApi userInstitutionApi) {
    this.userInstitutionApi = userInstitutionApi;
  }

  @Override
  public boolean verifyAllowedUserInstitution(String institutionId, String product, String uid) {
    log.trace("init verifyAllowedUserInstitution");

    if (Optional.ofNullable(institutionId).isEmpty()
        && Optional.ofNullable(product).isEmpty()
        && Optional.ofNullable(uid).isEmpty()) {
      throw new IllegalArgumentException("Input args empty");
    }

    String rolesFilter =
        new StringBuilder()
            .append(PartyRole.MANAGER.name())
            .append(",")
            .append(PartyRole.DELEGATE.name())
            .append(",")
            .append(PartyRole.SUB_DELEGATE.name())
            .toString();

    UserInstitutionRequest userInstitutionRequest =
        buildUserInstitutionRequest(
            institutionId, EMPTY, product, rolesFilter, ProductStatus.ACTIVE.name(), EMPTY);
    List<org.openapi.quarkus.user_json.model.UserInstitutionResponse> response =
        userInstitutionApi
            .institutionsInstitutionIdUserInstitutionsGet(
                userInstitutionRequest.getInstitutionId(),
                userInstitutionRequest.getProductRoles(),
                userInstitutionRequest.getProducts(),
                userInstitutionRequest.getRoles(),
                userInstitutionRequest.getStates(),
                userInstitutionRequest.getUserId())
            .await()
            .indefinitely();

    if (response == null || response.isEmpty()) {
      return false;
    }

    return response.stream()
        .anyMatch(
            currentElement -> {
              if (StringUtils.isNotBlank(currentElement.getUserId())) {
                return currentElement.getUserId().equalsIgnoreCase(uid);
              }
              return false;
            });
  }

  private UserInstitutionRequest buildUserInstitutionRequest(
      String institutionId,
      String productRole,
      String product,
      String role,
      String state,
      String userId) {
    return UserInstitutionRequest.builder()
        .institutionId(institutionId)
        .productRoles(splitValue(productRole))
        .products(splitValue(product))
        .roles(splitValue(role))
        .states(splitValue(state))
        .userId(userId)
        .build();
  }

  private List<String> splitValue(String value) {
    return StringUtils.isNotBlank(value) ? List.of(value.split(",")) : List.of(EMPTY);
  }
}
