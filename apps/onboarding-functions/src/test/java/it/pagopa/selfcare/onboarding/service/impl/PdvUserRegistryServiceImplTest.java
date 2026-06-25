package it.pagopa.selfcare.onboarding.service.impl;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.openapi.quarkus.user_registry_json.api.UserApi;
import org.openapi.quarkus.user_registry_json.model.UserResource;

class PdvUserRegistryServiceImplTest {

  @Test
  void getUserById_shouldDelegateToApi() {
    // given
    UserApi userApi = Mockito.mock(UserApi.class);
    PdvUserRegistryServiceImpl service = new PdvUserRegistryServiceImpl(userApi);
    UserResource expected = new UserResource();
    when(userApi.findByIdUsingGET("fields", "user-id")).thenReturn(expected);

    // when
    UserResource actual = service.getUserById("fields", "user-id");

    // then
    assertSame(expected, actual);
    verify(userApi).findByIdUsingGET("fields", "user-id");
  }
}
