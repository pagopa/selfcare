package it.pagopa.selfcare.onboarding.conf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.Arc;
import io.quarkus.mongodb.panache.common.MongoDatabaseResolver;
import io.quarkus.test.junit.QuarkusTest;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Pins the wiring rather than the logic.
 *
 * <p>Panache picks the resolver up through {@code Arc.container().select(MongoDatabaseResolver)}, so
 * the routing silently stops working if the bean stops being resolvable - for example if the
 * annotation is dropped or Quarkus removes it as unused. The unit tests would still pass in that
 * case, so this test asserts the container really hands our implementation to Panache, and that a
 * request carrying no product still lands on the configured shared database.
 */
@QuarkusTest
class OnboardingMongoDatabaseResolverWiringTest {

  @Test
  void panacheResolvesOurImplementationAndDefaultsToTheSharedDatabase() {
    Optional<MongoDatabaseResolver> selected =
        Optional.of(Arc.container().select(MongoDatabaseResolver.class))
            .filter(instance -> instance.isResolvable())
            .map(instance -> instance.get());

    assertTrue(selected.isPresent(), "Panache would find no MongoDatabaseResolver bean");
    assertInstanceOf(OnboardingMongoDatabaseResolver.class, selected.get());
    assertEquals("dummyOnboarding", selected.get().resolve());
  }
}
