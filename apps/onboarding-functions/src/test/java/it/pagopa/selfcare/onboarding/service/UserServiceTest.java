package it.pagopa.selfcare.onboarding.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.openapi.quarkus.user_json.api.InstitutionApi;
import org.openapi.quarkus.user_json.model.DeletedUserCountResponse;
import java.util.List;

import org.openapi.quarkus.user_json.model.UserInstitutionResponse;

@QuarkusTest
class UserServiceTest {

  @Inject
  UserService userService;

  @RestClient @InjectMock
  InstitutionApi institutionApi;

  private final String productId = "productId";
  private final String institutionId = "institutionId";

  @Test
  void deleteByIdAndInstitutionIdAndProductId() {
    // when
    DeletedUserCountResponse response = new DeletedUserCountResponse();
    response.setInstitutionId(institutionId);
    response.setProductId(productId);
    response.setDeletedUserCount(1L);
    when(institutionApi.deleteUserInstitutionProductUsers(any(), any())).thenReturn(response);

    userService.deleteByIdAndInstitutionIdAndProductId(institutionId, productId);

    Mockito.verify(institutionApi, times(1)).deleteUserInstitutionProductUsers(any(), any());

  }

  @Test
  void deleteUserWithException() {
    // when
    DeletedUserCountResponse response = new DeletedUserCountResponse();
    response.setInstitutionId(institutionId);
    response.setProductId(productId);
    response.setDeletedUserCount(0L);
    when(institutionApi.deleteUserInstitutionProductUsers(any(), any())).thenReturn(response);

    userService.deleteByIdAndInstitutionIdAndProductId(institutionId, productId);

    Mockito.verify(institutionApi, times(1)).deleteUserInstitutionProductUsers(any(), any());


  }

  @Test
  void deleteUserWithNullResponse() {
    // when
    when(institutionApi.deleteUserInstitutionProductUsers(any(), any())).thenReturn(null);

    userService.deleteByIdAndInstitutionIdAndProductId(institutionId, productId);

    Mockito.verify(institutionApi, times(1)).deleteUserInstitutionProductUsers(any(), any());


  }

  @Test
  void findEmailUuidByInstitutionAndProducts_whenApiReturnsNull_returnsEmpty() {
    List<String> products = List.of(productId);
    when(institutionApi.retrieveUserInstitutions(eq(institutionId), isNull(), eq(products), isNull(), isNull(), isNull()))
        .thenReturn(null);

    List<String> result = userService.findEmailUuidByInstitutionAndProducts(institutionId, products);

    assertNotNull(result);
    assertTrue(result.isEmpty());
    verify(institutionApi, times(1))
        .retrieveUserInstitutions(eq(institutionId), isNull(), eq(products), isNull(), isNull(), isNull());
  }

  @Test
  void findEmailUuidByInstitutionAndProducts_whenApiReturnsEmpty_returnsEmpty() {
    List<String> products = List.of(productId);
    when(institutionApi.retrieveUserInstitutions(eq(institutionId), isNull(), eq(products), isNull(), isNull(), isNull()))
        .thenReturn(List.of());

    List<String> result = userService.findEmailUuidByInstitutionAndProducts(institutionId, products);

    assertNotNull(result);
    assertTrue(result.isEmpty());
    verify(institutionApi, times(1))
        .retrieveUserInstitutions(eq(institutionId), isNull(), eq(products), isNull(), isNull(), isNull());
  }

  @Test
  void findEmailUuidByInstitutionAndProducts_whenEmailsBlankOrNull_returnsEmpty() {
    List<String> products = List.of(productId);
    UserInstitutionResponse blankEmail = createUserInstitutionResponse(" ");
    UserInstitutionResponse nullEmail = createUserInstitutionResponse(null);
    when(institutionApi.retrieveUserInstitutions(eq(institutionId), isNull(), eq(products), isNull(), isNull(), isNull()))
        .thenReturn(List.of(blankEmail, nullEmail));

    List<String> result = userService.findEmailUuidByInstitutionAndProducts(institutionId, products);

    assertNotNull(result);
    assertTrue(result.isEmpty());
    verify(institutionApi, times(1))
        .retrieveUserInstitutions(eq(institutionId), isNull(), eq(products), isNull(), isNull(), isNull());
  }

  @Test
  void findEmailUuidByInstitutionAndProducts_filtersDuplicatesAndBlanks() {
    List<String> products = List.of(productId);
    UserInstitutionResponse first = createUserInstitutionResponse("email-uuid-1");
    UserInstitutionResponse duplicate = createUserInstitutionResponse("email-uuid-1");
    UserInstitutionResponse second = createUserInstitutionResponse("email-uuid-2");
    UserInstitutionResponse blank = createUserInstitutionResponse("  ");
    when(institutionApi.retrieveUserInstitutions(eq(institutionId), isNull(), eq(products), isNull(), isNull(), isNull()))
        .thenReturn(List.of(first, duplicate, second, blank));

    List<String> result = userService.findEmailUuidByInstitutionAndProducts(institutionId, products);

    assertEquals(List.of("email-uuid-1", "email-uuid-2"), result);
    verify(institutionApi, times(1))
        .retrieveUserInstitutions(eq(institutionId), isNull(), eq(products), isNull(), isNull(), isNull());
  }

  @Test
  void findEmailUuidByInstitutionAndProducts_returnsEmails() {
    List<String> products = List.of(productId);
    UserInstitutionResponse first = createUserInstitutionResponse("email-uuid-1");
    UserInstitutionResponse second = createUserInstitutionResponse("email-uuid-2");
    when(institutionApi.retrieveUserInstitutions(eq(institutionId), isNull(), eq(products), isNull(), isNull(), isNull()))
        .thenReturn(List.of(first, second));

    List<String> result = userService.findEmailUuidByInstitutionAndProducts(institutionId, products);

    assertEquals(List.of("email-uuid-1", "email-uuid-2"), result);
    verify(institutionApi, times(1))
        .retrieveUserInstitutions(eq(institutionId), isNull(), eq(products), isNull(), isNull(), isNull());
  }

  private UserInstitutionResponse createUserInstitutionResponse(String emailUuid) {
    UserInstitutionResponse response = new UserInstitutionResponse();
    response.setUserMailUuid(emailUuid);
    return response;
  }

}
