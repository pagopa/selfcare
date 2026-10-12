package it.pagopa.selfcare.onboarding.parity;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

/**
 * Starts the controlled downstream before the application and points every REST client, the API
 * keys and the JWT verification key to it. The stub is handed to the test through {@link Stub}.
 */
public class ParityTestEnvironment implements QuarkusTestResourceLifecycleManager {

  /** Receives the {@link DownstreamStub} of this run. */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.FIELD)
  public @interface Stub {}

  private DownstreamStub stub;

  @Override
  public Map<String, String> start() {
    stub = new DownstreamStub();
    return ParityTargets.quarkusConfig(stub);
  }

  @Override
  public void inject(TestInjector testInjector) {
    testInjector.injectIntoFields(
        stub, new TestInjector.AnnotatedAndMatchesType(Stub.class, DownstreamStub.class));
  }

  @Override
  public void stop() {
    if (stub != null) {
      stub.close();
    }
  }
}
