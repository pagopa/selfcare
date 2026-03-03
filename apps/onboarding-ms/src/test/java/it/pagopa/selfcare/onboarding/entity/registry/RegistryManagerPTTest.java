package it.pagopa.selfcare.onboarding.entity.registry;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.onboarding.common.WorkflowType;
import it.pagopa.selfcare.onboarding.entity.Institution;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.User;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.exception.OnboardingNotAllowedException;
import it.pagopa.selfcare.product.entity.Product;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RegistryManagerPTTest {

  private Onboarding onboarding;
  private RegistryManagerPT registryManagerPT;
  private Product product;

  @BeforeEach
  void setUp() {
    onboarding = createDummyOnboarding();
    registryManagerPT = new RegistryManagerPT(onboarding);
    product = mock(Product.class);
  }

  @Test
  void customValidationTest() {
    // given
    when(product.isDelegable()).thenReturn(true);
    onboarding.setWorkflowType(WorkflowType.FOR_APPROVE_PT);

    // when
    Uni<Onboarding> result = registryManagerPT.customValidation(product);

    // then
    assertTrue(Objects.nonNull(result.await().indefinitely()));
  }

  @Test
  void customValidationTest_shouldThrowNewInvalidRequestException() {
    // given
    when(product.isDelegable()).thenReturn(true);
    when(product.getParentId()).thenReturn(null);

    // when
    Uni<Onboarding> result = registryManagerPT.customValidation(product);

    // then
    assertThrows(InvalidRequestException.class, () -> result.await().indefinitely());
  }

  @Test
  void customValidationTest_shouldThrowOnboardingNotAllowedException_whenProductIsNotDelegable() {
    // given
    when(product.isDelegable()).thenReturn(false);

    // when
    Uni<Onboarding> result = registryManagerPT.customValidation(product);

    // then
    assertThrows(OnboardingNotAllowedException.class, () -> result.await().indefinitely());
  }

  private Onboarding createDummyOnboarding() {
    Onboarding onboarding = new Onboarding();
    onboarding.setId(UUID.randomUUID().toString());
    onboarding.setProductId("prod-io");

    Institution institution = new Institution();
    institution.setTaxCode("taxCode");
    institution.setSubunitCode("subunitCode");
    institution.setInstitutionType(InstitutionType.PSP);
    onboarding.setInstitution(institution);

    User user = new User();
    user.setId("actual-user-id");
    user.setRole(PartyRole.MANAGER);
    onboarding.setUsers(List.of(user));
    return onboarding;
  }
}
