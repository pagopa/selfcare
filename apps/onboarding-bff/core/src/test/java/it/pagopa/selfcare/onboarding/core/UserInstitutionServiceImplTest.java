package it.pagopa.selfcare.onboarding.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.onboarding.connector.api.UserInstitutionConnector;
import it.pagopa.selfcare.onboarding.connector.model.userInstitution.UserInstitutionRequest;
import it.pagopa.selfcare.onboarding.connector.model.userInstitution.UserInstitutionResponse;
import it.pagopa.selfcare.product.entity.ProductStatus;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserInstitutionServiceImplTest {

  @InjectMocks private UserInstitutionServiceImpl userInstitutionService;

  @Mock private UserInstitutionConnector userInstitutionConnector;

  @Test
  void verifyAllowedUserInstitution_shouldReturnEmptyList() {
    // given
    String institutionId = "institutionId";
    String product = "product";
    String uid = "uid";

    UserInstitutionRequest userInstitutionRequest = dummyUserInstitutionRequest();

    List<UserInstitutionResponse> userInstitutionResponseList = new ArrayList<>(List.of());

    when(userInstitutionConnector.getInstitutionUsersByFilter(userInstitutionRequest))
        .thenReturn(userInstitutionResponseList);

    // when
    boolean result =
        userInstitutionService.verifyAllowedUserInstitution(institutionId, product, uid);

    // then
    assertFalse(result);
  }

  @Test
  void verifyAllowedUserInstitution_shouldReturnUserNotPresentAndNotEmptyList() {
    // given
    String institutionId = "institutionId";
    String product = "product";
    String uid = "uid";

    UserInstitutionRequest userInstitutionRequest = dummyUserInstitutionRequest();

    List<UserInstitutionResponse> userInstitutionResponseList = new ArrayList<>(List.of());
    userInstitutionResponseList.add(new UserInstitutionResponse());

    when(userInstitutionConnector.getInstitutionUsersByFilter(userInstitutionRequest))
        .thenReturn(userInstitutionResponseList);

    // when
    boolean result =
        userInstitutionService.verifyAllowedUserInstitution(institutionId, product, uid);

    // then
    assertFalse(result);
  }

  @Test
  void verifyAllowedUserInstitution_shouldReturnUserInList() {
    // given
    String institutionId = "institutionId";
    String product = "product";
    String uid = "PRESENTE";

    UserInstitutionRequest userInstitutionRequest = dummyUserInstitutionRequest();

    List<UserInstitutionResponse> userInstitutionResponseList = new ArrayList<>(List.of());

    UserInstitutionResponse userInstitutionResponse = new UserInstitutionResponse();
    userInstitutionResponse.setUserId(uid);

    userInstitutionResponseList.add(userInstitutionResponse);

    when(userInstitutionConnector.getInstitutionUsersByFilter(userInstitutionRequest))
        .thenReturn(userInstitutionResponseList);

    // when
    boolean result =
        userInstitutionService.verifyAllowedUserInstitution(institutionId, product, uid);

    // then
    assertTrue(result);
  }

  @Test
  void verifyAllowedUserInstitution_shouldReturnUserNotInList() {
    // given
    String institutionId = "institutionId";
    String product = "product";
    String uid = "NON E PRESENTE";

    UserInstitutionRequest userInstitutionRequest = dummyUserInstitutionRequest();

    List<UserInstitutionResponse> userInstitutionResponseList = new ArrayList<>(List.of());
    UserInstitutionResponse userInstitutionResponse = new UserInstitutionResponse();
    userInstitutionResponse.setUserId("NON PRESENTE");

    userInstitutionResponseList.add(userInstitutionResponse);

    when(userInstitutionConnector.getInstitutionUsersByFilter(userInstitutionRequest))
        .thenReturn(userInstitutionResponseList);

    // when
    boolean result =
        userInstitutionService.verifyAllowedUserInstitution(institutionId, product, uid);

    // then
    assertFalse(result);
  }

  @Test
  void verifyAllowedUserInstitution_shouldReturnException() {
    // given

    // when
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          userInstitutionService.verifyAllowedUserInstitution(null, null, null);
        });
    // then

  }

  private UserInstitutionRequest dummyUserInstitutionRequest() {
    return UserInstitutionRequest.builder()
        .institutionId("institutionId")
        .productRoles(List.of(""))
        .products(List.of("product"))
        .roles(
            List.of(
                PartyRole.MANAGER.name(), PartyRole.DELEGATE.name(), PartyRole.SUB_DELEGATE.name()))
        .states(List.of(ProductStatus.ACTIVE.name()))
        .userId(StringUtils.EMPTY)
        .build();
  }
}
