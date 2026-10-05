package it.pagopa.selfcare.document.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.tenant.TenantRegistry;
import jakarta.enterprise.event.Observes;
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

    @Test
    void validatorBeanIsRegistered() {
        assertTrue(Arc.container().select(TenantRegistryStartupValidator.class).isResolvable());
    }

    @Test
    void startupObserverInjectsTheTenantRegistry() {
        boolean observesStartupWithRegistry = Arrays.stream(TenantRegistryStartupValidator.class.getDeclaredMethods())
                .anyMatch(TenantRegistryStartupValidatorTest::observesStartupWithRegistry);

        assertTrue(observesStartupWithRegistry,
                "TenantRegistryStartupValidator must observe StartupEvent and inject TenantRegistry");
    }

    private static boolean observesStartupWithRegistry(Method method) {
        Parameter[] parameters = method.getParameters();
        boolean observesStartup = Arrays.stream(parameters)
                .anyMatch(p -> p.getType().equals(StartupEvent.class) && p.isAnnotationPresent(Observes.class));
        boolean injectsRegistry = Arrays.stream(parameters)
                .anyMatch(p -> p.getType().equals(TenantRegistry.class));
        return observesStartup && injectsRegistry;
    }
}
