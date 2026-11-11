package it.pagopa.selfcare.onboarding.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.onboarding.context.TenantJwtSessionAudiences;
import it.pagopa.selfcare.tenant.TenantRegistry;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/**
 * Pins the fail-closed contract: TenantRegistry is lazy and validates itself in @PostConstruct, so
 * an invalid registry blocks startup only if a StartupEvent observer injects it.
 */
@QuarkusTest
class TenantRegistryStartupValidatorTest {

  @Inject TenantRegistry registry;
  @Inject TenantJwtSessionAudiences audiences;

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
    assertDoesNotThrow(() -> TenantRegistryStartupValidator.validate(registry, audiences));
  }

  @Test
  void failsWhenASupportedTenantHasNoSessionAudience() {
    TenantJwtSessionAudiences arOnly =
        new TenantJwtSessionAudiences(
            registry, "{\"AR\":{\"jwt\":{\"session\":{\"audience\":\"api.dev.selfcare.pagopa.it\"}}}}");

    assertThrows(
        IllegalStateException.class, () -> TenantRegistryStartupValidator.validate(registry, arOnly));
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
