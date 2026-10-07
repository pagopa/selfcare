package it.pagopa.selfcare.onboarding.parity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;

/** The public OpenAPI document of the Spring BFF at the migrated baseline: the reference contract. */
public final class SpringSpec {

  public static final String RESOURCE = "/parity/spring-api-docs.json";

  private SpringSpec() {}

  public static JsonNode load() {
    try (InputStream in = SpringSpec.class.getResourceAsStream(RESOURCE)) {
      if (in == null) {
        throw new IllegalStateException("missing test resource " + RESOURCE);
      }
      return new ObjectMapper().readTree(in);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
