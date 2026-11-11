package it.pagopa.selfcare.onboarding.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.tenant.TenantRegistry;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Pins the fail-closed contract: TenantRegistry is lazy and validates itself in @PostConstruct, so
 * an invalid registry blocks startup only if a StartupEvent observer injects it.
 */
@QuarkusTest
class TenantRegistryStartupValidatorTest {

  @Inject TenantRegistry registry;

  @Test
  void validatorBeanIsRegistered() {
    assertTrue(Arc.container().select(TenantRegistryStartupValidator.class).isResolvable());
  }

  @Test
  void startupObserverInjectsTheTenantRegistry() {
    boolean observesStartupWithRegistry =
        Arrays.stream(TenantRegistryStartupValidator.class.getDeclaredMethods())
            .anyMatch(TenantRegistryStartupValidatorTest::observesStartupWithRegistry);

    assertTrue(
        observesStartupWithRegistry,
        "TenantRegistryStartupValidator must observe StartupEvent and inject TenantRegistry");
  }

  @Test
  void acceptsRegistryWithAudienceForEverySupportedTenant() {
    assertDoesNotThrow(() -> TenantRegistryStartupValidator.validate(registry));
  }

  @Test
  void failsWhenASupportedTenantHasNoSessionAudience() {
    TenantRegistry arOnly = mock(TenantRegistry.class);
    when(arOnly.supportedTenantIds()).thenReturn(Set.of("AR", "PNPG"));
    when(arOnly.jwtSessionAudience("AR")).thenReturn(Optional.of("api.dev.selfcare.pagopa.it"));
    when(arOnly.jwtSessionAudience("PNPG")).thenReturn(Optional.empty());

    assertThrows(IllegalStateException.class, () -> TenantRegistryStartupValidator.validate(arOnly));
  }

  private static boolean observesStartupWithRegistry(Method method) {
    Parameter[] parameters = method.getParameters();
    boolean observesStartup =
        Arrays.stream(parameters)
            .anyMatch(
                p -> p.getType().equals(StartupEvent.class) && p.isAnnotationPresent(Observes.class));
    boolean injectsRegistry =
        Arrays.stream(parameters).anyMatch(p -> p.getType().equals(TenantRegistry.class));
    return observesStartup && injectsRegistry;
  }
}
