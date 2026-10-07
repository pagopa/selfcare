package it.pagopa.selfcare.onboarding.parity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * The oracle check: the same catalog against the unchanged Spring executable jar. Enabled with
 * {@code -Dparity.spring.jar=<path to onboarding-bff-*-FATJAR.jar built from the Spring baseline>}.
 * A scenario that fails here is a wrong expectation, never a Quarkus defect.
 */
@EnabledIfSystemProperty(
    named = SpringReference.JAR_PROPERTY,
    matches = ".+",
    disabledReason =
        "Spring oracle not run: -Dparity.spring.jar is not set. The catalog expectations are validated on the Spring jar, "
            + "never assumed; ParityCatalogTest still guards the catalog size.")
class SpringReferenceParityTest {

  private static DownstreamStub stub;
  private static SpringReference spring;
  private static final AtomicInteger ATTEMPTED = new AtomicInteger();
  private static final AtomicInteger FAILED = new AtomicInteger();

  @BeforeAll
  static void startReference() throws Exception {
    stub = new DownstreamStub();
    spring = SpringReference.start(Path.of(System.getProperty(SpringReference.JAR_PROPERTY)), stub);
  }

  @AfterAll
  static void stopReference() throws Exception {
    int attempted = ATTEMPTED.get();
    System.out.println(
        "PARITY spring-reference scenarios="
            + ParityCatalog.selected().size()
            + " attempted="
            + attempted
            + " passed="
            + (attempted - FAILED.get())
            + " failed="
            + FAILED.get());
    try {
      if (spring != null) {
        spring.close();
      }
    } finally {
      if (stub != null) {
        stub.close();
      }
    }
    assertEquals(ParityCatalog.selected().size(), attempted, "not every parity scenario was executed");
  }

  @TestFactory
  Stream<DynamicTest> scenariosHoldOnTheSpringReference() {
    List<Scenario> all = ParityCatalog.selected();
    assertFalse(all.isEmpty(), "the parity catalog is empty: nothing would be verified");
    return all.stream()
        .map(
            scenario ->
                DynamicTest.dynamicTest(
                    scenario.displayName(),
                    () -> {
                      ATTEMPTED.incrementAndGet();
                      try {
                        scenario.run(spring.baseUrl(), stub);
                      } catch (AssertionError | RuntimeException e) {
                        FAILED.incrementAndGet();
                        throw e;
                      }
                    }));
  }
}
