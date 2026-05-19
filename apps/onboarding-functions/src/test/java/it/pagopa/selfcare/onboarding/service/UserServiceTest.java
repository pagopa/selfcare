package it.pagopa.selfcare.onboarding.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.onboarding.dto.UserMail;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.openapi.quarkus.core_json.model.OnboardedProductResponse;
import org.openapi.quarkus.user_json.api.InstitutionApi;
import org.openapi.quarkus.user_json.api.UserApi;
import org.openapi.quarkus.user_json.model.DeletedUserCountResponse;
import java.util.List;

import org.openapi.quarkus.user_json.model.SendMailDto;
import org.openapi.quarkus.user_json.model.UserInstitutionResponse;

@QuarkusTest
class UserServiceTest {

  @Inject
  UserService userService;

  @RestClient @InjectMock
  InstitutionApi institutionApi;
  @RestClient @InjectMock
  UserApi userApi;

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
  void findEmailByInstitutionAndProducts_whenApiReturnsNull_returnsEmpty() {
    List<String> products = List.of(productId);
    when(institutionApi.retrieveUserInstitutions(eq(institutionId), isNull(), eq(products), isNull(), isNull(), isNull()))
        .thenReturn(null);

    List<UserMail> result = userService.findEmailByInstitutionAndProducts(institutionId, products);

    assertNotNull(result);
    assertTrue(result.isEmpty());
    verify(institutionApi, times(1))
        .retrieveUserInstitutions(eq(institutionId), isNull(), eq(products), isNull(), isNull(), isNull());
  }

  @Test
  void findEmailByInstitutionAndProducts_whenApiReturnsEmpty_returnsEmpty() {
    List<String> products = List.of(productId);
    when(institutionApi.retrieveUserInstitutions(eq(institutionId), isNull(), eq(products), isNull(), isNull(), isNull()))
        .thenReturn(List.of());

    List<UserMail> result = userService.findEmailByInstitutionAndProducts(institutionId, products);

    assertNotNull(result);
    assertTrue(result.isEmpty());
    verify(institutionApi, times(1))
        .retrieveUserInstitutions(eq(institutionId), isNull(), eq(products), isNull(), isNull(), isNull());
  }

  @Test
  void findEmailByInstitutionAndProducts_whenEmailsBlankOrNull_returnsEmpty() {
    List<String> products = List.of(productId);
    UserInstitutionResponse blankEmail = createUserInstitutionResponse(" ", " ");
    UserInstitutionResponse nullEmail = createUserInstitutionResponse(null, null);
    when(institutionApi.retrieveUserInstitutions(eq(institutionId), isNull(), eq(products), isNull(), isNull(), isNull()))
        .thenReturn(List.of(blankEmail, nullEmail));

    List<UserMail> result = userService.findEmailByInstitutionAndProducts(institutionId, products);

    assertNotNull(result);
    assertTrue(result.isEmpty());
    verify(institutionApi, times(1))
        .retrieveUserInstitutions(eq(institutionId), isNull(), eq(products), isNull(), isNull(), isNull());
  }

  @Test
  void findEmailByInstitutionAndProducts_filtersBlanks() {
    List<String> products = List.of(productId);
    UserInstitutionResponse first = createUserInstitutionResponse("email-uuid-1", "user-1");
    UserInstitutionResponse second = createUserInstitutionResponse("email-uuid-2",  "user-3");
    UserInstitutionResponse blank = createUserInstitutionResponse("  ", " ");
    when(institutionApi.retrieveUserInstitutions(eq(institutionId), isNull(), eq(products), isNull(), isNull(), isNull()))
        .thenReturn(List.of(first, second, blank));

    List<UserMail> result = userService.findEmailByInstitutionAndProducts(institutionId, products);

    assertEquals(List.of("email-uuid-1", "email-uuid-2"), result.stream().map(UserMail::getUserMailUuid).toList());
    assertEquals(List.of("user-1", "user-3"), result.stream().map(UserMail::getUserId).toList());
    verify(institutionApi, times(1))
        .retrieveUserInstitutions(eq(institutionId), isNull(), eq(products), isNull(), isNull(), isNull());
  }

  @Test
  void findEmailByInstitutionAndProducts_returnsEmails() {
    List<String> products = List.of(productId);
    UserInstitutionResponse first = createUserInstitutionResponse("email-uuid-1",  "user-1");
    UserInstitutionResponse second = createUserInstitutionResponse("email-uuid-2",   "user-2");
    when(institutionApi.retrieveUserInstitutions(eq(institutionId), isNull(), eq(products), isNull(), isNull(), isNull()))
        .thenReturn(List.of(first, second));

    List<UserMail> result = userService.findEmailByInstitutionAndProducts(institutionId, products);

    assertEquals(List.of("email-uuid-1", "email-uuid-2"), result.stream().map(UserMail::getUserMailUuid).toList());
    assertEquals(List.of("user-1", "user-2"), result.stream().map(UserMail::getUserId).toList());
    verify(institutionApi, times(1))
        .retrieveUserInstitutions(eq(institutionId), isNull(), eq(products), isNull(), isNull(), isNull());
  }

  @Test
  void getActiveManagersByInstitutionAndProduct_shouldCallApiWithExpectedFilters() {
    // given
    List<UserInstitutionResponse> expected = List.of(new UserInstitutionResponse());
    when(
      institutionApi.retrieveUserInstitutions(
        eq("institution-id"),
        isNull(),
        eq(List.of("product-id")),
        eq(List.of(String.valueOf(PartyRole.MANAGER))),
        eq(List.of(String.valueOf(OnboardedProductResponse.StatusEnum.ACTIVE))),
        isNull()))
      .thenReturn(expected);

    // when
    List<UserInstitutionResponse> actual = userService.getActiveManagersByInstitutionAndProduct(
      "institution-id",
      "product-id",
      OnboardedProductResponse.StatusEnum.ACTIVE);

    // then
    assertEquals(expected, actual);
    verify(institutionApi)
      .retrieveUserInstitutions(
        eq("institution-id"),
        isNull(),
        eq(List.of("product-id")),
        eq(List.of(String.valueOf(PartyRole.MANAGER))),
        eq(List.of(String.valueOf(OnboardedProductResponse.StatusEnum.ACTIVE))),
        isNull());
  }

  @Test
  void sendMailRequest_shouldDelegateToApi() {
    // given
    SendMailDto mailDto = new SendMailDto();

    // when
    userService.sendMailRequest("user-id", mailDto);

    // then
    verify(userApi).sendMailRequest("user-id", mailDto);
  }

  @Test
  void sendMailRequest_shouldNotThrowWhenApiFails() {
    // given
    SendMailDto mailDto = new SendMailDto();
    doThrow(new RuntimeException("boom")).when(userApi).sendMailRequest("user-id", mailDto);

    // when
    userService.sendMailRequest("user-id", mailDto);

    // then
    verify(userApi).sendMailRequest("user-id", mailDto);
  }

  private UserInstitutionResponse createUserInstitutionResponse(String emailUuid, String userId) {
    UserInstitutionResponse response = new UserInstitutionResponse();
    response.setUserMailUuid(emailUuid);
    response.setUserId(userId);
    return response;
  }

}
