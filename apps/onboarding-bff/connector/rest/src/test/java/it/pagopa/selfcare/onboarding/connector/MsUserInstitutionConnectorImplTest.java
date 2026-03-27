package it.pagopa.selfcare.onboarding.connector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import it.pagopa.selfcare.onboarding.connector.model.userInstitution.UserInstitutionRequest;
import it.pagopa.selfcare.onboarding.connector.model.userInstitution.UserInstitutionResponse;
import it.pagopa.selfcare.onboarding.connector.rest.client.MsUserInstitutionApiClient;
import it.pagopa.selfcare.product.entity.ProductStatus;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {MsUserInstitutionConnectorImpl.class})
@ExtendWith(MockitoExtension.class)
class MsUserInstitutionConnectorImplTest {

  @InjectMocks private MsUserInstitutionConnectorImpl msUserInstitutionConnector;

  @Mock private MsUserInstitutionApiClient userInstitutionApiClient;

  @Test
  void getInstitutionUsersByFilterTest_shouldReturnEmptyList() {
    // given
    UserInstitutionRequest userInstitutionRequest = dummyUserInstitutionRequest();

    List<it.pagopa.selfcare.user.generated.openapi.v1.dto.UserInstitutionResponse>
        userInstitutionResponses = new ArrayList<>();

    when(userInstitutionApiClient._institutionsInstitutionIdUserInstitutionsGet(
            anyString(), anyList(), anyList(), anyList(), anyList(), anyString()))
        .thenReturn(new ResponseEntity<>(userInstitutionResponses, HttpStatus.OK));

    // when
    List<UserInstitutionResponse> response =
        msUserInstitutionConnector.getInstitutionUsersByFilter(userInstitutionRequest);

    // then
    assertTrue(response.isEmpty());
  }

  @Test
  void getInstitutionUsersByFilterTest_shouldReturnNotEmptyList() {
    // given
    UserInstitutionRequest userInstitutionRequest = dummyUserInstitutionRequest();

    List<it.pagopa.selfcare.user.generated.openapi.v1.dto.UserInstitutionResponse>
        userInstitutionResponses = new ArrayList<>();
    userInstitutionResponses.add(
        new it.pagopa.selfcare.user.generated.openapi.v1.dto.UserInstitutionResponse());

    when(userInstitutionApiClient._institutionsInstitutionIdUserInstitutionsGet(
            anyString(), anyList(), anyList(), anyList(), anyList(), anyString()))
        .thenReturn(new ResponseEntity<>(userInstitutionResponses, HttpStatus.OK));

    // when
    List<UserInstitutionResponse> response =
        msUserInstitutionConnector.getInstitutionUsersByFilter(userInstitutionRequest);

    // then
    assertFalse(response.isEmpty());
    assertEquals(1, response.size());
  }

  private UserInstitutionRequest dummyUserInstitutionRequest() {
    return UserInstitutionRequest.builder()
        .institutionId("institutionId")
        .productRoles(List.of(""))
        .products(List.of("product"))
        .roles(List.of(""))
        .states(List.of(ProductStatus.ACTIVE.name()))
        .userId(StringUtils.EMPTY)
        .build();
  }
}
