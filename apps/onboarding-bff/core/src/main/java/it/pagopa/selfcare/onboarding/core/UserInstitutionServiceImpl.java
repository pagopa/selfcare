package it.pagopa.selfcare.onboarding.core;

import static org.apache.commons.lang3.StringUtils.*;

import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.onboarding.connector.api.UserInstitutionConnector;
import it.pagopa.selfcare.onboarding.connector.model.userInstitution.UserInstitutionRequest;
import it.pagopa.selfcare.onboarding.connector.model.userInstitution.UserInstitutionResponse;
import it.pagopa.selfcare.product.entity.ProductStatus;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UserInstitutionServiceImpl implements UserInstitutionService {

  private final UserInstitutionConnector userInstitutionConnector;

  @Autowired
  public UserInstitutionServiceImpl(UserInstitutionConnector userInstitutionConnector) {
    this.userInstitutionConnector = userInstitutionConnector;
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
    List<UserInstitutionResponse> response =
        userInstitutionConnector.getInstitutionUsersByFilter(userInstitutionRequest);

    if (response.isEmpty()) {
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
