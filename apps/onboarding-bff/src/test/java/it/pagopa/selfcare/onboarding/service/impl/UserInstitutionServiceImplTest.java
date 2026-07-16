package it.pagopa.selfcare.onboarding.service.impl;

import it.pagopa.selfcare.onboarding.service.*;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.product.entity.ProductStatus;
import io.smallrye.mutiny.Uni;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapi.quarkus.user_json.api.InstitutionControllerApi;
import org.openapi.quarkus.user_json.model.UserInstitutionResponse;

@ExtendWith(MockitoExtension.class)
class UserInstitutionServiceImplTest {

  @InjectMocks private UserInstitutionServiceImpl userInstitutionService;

  @Mock private InstitutionControllerApi userInstitutionApi;

  @Test
  void verifyAllowedUserInstitution_shouldReturnEmptyList() {
    // given
    String institutionId = "institutionId";
    String product = "product";
    String uid = "uid";

    List<UserInstitutionResponse> userInstitutionResponseList = new ArrayList<>(List.of());
    when(userInstitutionApi.institutionsInstitutionIdUserInstitutionsGet(
            "institutionId",
            List.of(""),
            List.of("product"),
            List.of(PartyRole.MANAGER.name(), PartyRole.DELEGATE.name(), PartyRole.SUB_DELEGATE.name()),
            List.of(ProductStatus.ACTIVE.name()),
            StringUtils.EMPTY))
        .thenReturn(Uni.createFrom().item(userInstitutionResponseList));

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

    List<UserInstitutionResponse> userInstitutionResponseList = new ArrayList<>(List.of());
    userInstitutionResponseList.add(new UserInstitutionResponse());

    when(userInstitutionApi.institutionsInstitutionIdUserInstitutionsGet(
            "institutionId",
            List.of(""),
            List.of("product"),
            List.of(PartyRole.MANAGER.name(), PartyRole.DELEGATE.name(), PartyRole.SUB_DELEGATE.name()),
            List.of(ProductStatus.ACTIVE.name()),
            StringUtils.EMPTY))
        .thenReturn(Uni.createFrom().item(userInstitutionResponseList));

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

    List<UserInstitutionResponse> userInstitutionResponseList = new ArrayList<>(List.of());

    UserInstitutionResponse userInstitutionResponse = new UserInstitutionResponse();
    userInstitutionResponse.setUserId(uid);

    userInstitutionResponseList.add(userInstitutionResponse);

    when(userInstitutionApi.institutionsInstitutionIdUserInstitutionsGet(
            "institutionId",
            List.of(""),
            List.of("product"),
            List.of(PartyRole.MANAGER.name(), PartyRole.DELEGATE.name(), PartyRole.SUB_DELEGATE.name()),
            List.of(ProductStatus.ACTIVE.name()),
            StringUtils.EMPTY))
        .thenReturn(Uni.createFrom().item(userInstitutionResponseList));

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

    List<UserInstitutionResponse> userInstitutionResponseList = new ArrayList<>(List.of());
    UserInstitutionResponse userInstitutionResponse = new UserInstitutionResponse();
    userInstitutionResponse.setUserId("NON PRESENTE");

    userInstitutionResponseList.add(userInstitutionResponse);

    when(userInstitutionApi.institutionsInstitutionIdUserInstitutionsGet(
            "institutionId",
            List.of(""),
            List.of("product"),
            List.of(PartyRole.MANAGER.name(), PartyRole.DELEGATE.name(), PartyRole.SUB_DELEGATE.name()),
            List.of(ProductStatus.ACTIVE.name()),
            StringUtils.EMPTY))
        .thenReturn(Uni.createFrom().item(userInstitutionResponseList));

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

}
