package it.pagopa.selfcare.onboarding.parity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The migrated BFF, over real HTTP with really signed tokens, against the controlled downstream:
 * every scenario of the {@link ParityCatalog}, each one validated beforehand on the unchanged
 * Spring BFF ({@link SpringReferenceParityTest}).
 */
@QuarkusTest
@TestProfile(ParityTestProfile.class)
class QuarkusParityTest {

  private static final AtomicInteger ATTEMPTED = new AtomicInteger();
  private static final AtomicInteger FAILED = new AtomicInteger();

  @ParityTestEnvironment.Stub DownstreamStub stub;

  @TestHTTPResource String baseUrl;

  static Stream<Named<Scenario>> scenarios() {
    List<Scenario> all = ParityCatalog.selected();
    assertFalse(all.isEmpty(), "the parity catalog is empty: nothing would be verified");
    return all.stream().map(scenario -> Named.of(scenario.displayName(), scenario));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("scenarios")
  void behavesLikeTheSpringReference(Scenario scenario) {
    ATTEMPTED.incrementAndGet();
    try {
      scenario.run(baseUrl, stub);
    } catch (AssertionError | RuntimeException e) {
      FAILED.incrementAndGet();
      throw e;
    }
  }

  @AfterAll
  static void everyScenarioRan() {
    int attempted = ATTEMPTED.get();
    System.out.println(
        "PARITY quarkus scenarios="
            + ParityCatalog.selected().size()
            + " attempted="
            + attempted
            + " passed="
            + (attempted - FAILED.get())
            + " failed="
            + FAILED.get());
    assertEquals(ParityCatalog.selected().size(), attempted, "not every parity scenario was executed");
  }
}
