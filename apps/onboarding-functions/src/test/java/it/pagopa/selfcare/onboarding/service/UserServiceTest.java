package it.pagopa.selfcare.onboarding.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.onboarding.entity.Institution;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.User;
import it.pagopa.selfcare.onboarding.repository.OnboardingRepository;
import jakarta.inject.Inject;
import java.util.List;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.openapi.quarkus.user_json.api.InstitutionApi;
import org.openapi.quarkus.user_json.model.DeletedUserCountResponse;

@QuarkusTest
class UserServiceTest {

  @Inject
  UserService userService;

  @RestClient @InjectMock
  InstitutionApi institutionApi;

  @InjectMock
  OnboardingRepository onboardingRepository;

  private final String productId = "productId";
  private final String institutionId = "institutionId";

  @Test
  void findByInstitutionAndProduct() {
    // given
    Onboarding onboarding = new Onboarding();
    Institution institution = new Institution();
    institution.setId(institutionId);
    onboarding.setInstitution(institution);
    onboarding.setProductId(productId);
    onboarding.setUsers(List.of());
    // when
    when(onboardingRepository.findByOnboardingUsers(institutionId, productId))
        .thenReturn(List.of(onboarding));
    // then
    List<String> onboardings = userService.findByInstitutionAndProduct(institutionId, productId);
    assertNotNull(onboardings);
    assertTrue(onboardings.isEmpty());

    Mockito.verify(onboardingRepository, times(1))
            .findByOnboardingUsers(institutionId, productId);

  }

  @Test
  void findByInstitutionAndProduct_NotEmptyList() {
    // given
    Onboarding onboarding = new Onboarding();
    Institution institution = new Institution();
    institution.setId(institutionId);
    onboarding.setInstitution(institution);
    onboarding.setProductId(productId);
    User user = new User();
    String userId = "userId";
    user.setId(userId);
    onboarding.setUsers(List.of(user));
    // when
    when(onboardingRepository.findByOnboardingUsers(institutionId, productId))
            .thenReturn(List.of(onboarding));
    // then
    List<String> onboardings = userService.findByInstitutionAndProduct(institutionId, productId);
    assertNotNull(onboardings);
    assertFalse(onboardings.isEmpty());
    assertEquals(1, onboardings.size());
    assertEquals(userId, onboardings.get(0));

    Mockito.verify(onboardingRepository, times(1))
            .findByOnboardingUsers(institutionId, productId);

  }

  @Test
  void findByInstitutionAndProduct_EmptyList() {
    // when
    when(onboardingRepository.findByOnboardingUsers(institutionId, productId))
            .thenReturn(List.of());
    // then
    List<String> onboardings = userService.findByInstitutionAndProduct(institutionId, productId);
    assertNotNull(onboardings);
    assertTrue(onboardings.isEmpty());

    Mockito.verify(onboardingRepository, times(1))
            .findByOnboardingUsers(institutionId, productId);

  }

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

}
