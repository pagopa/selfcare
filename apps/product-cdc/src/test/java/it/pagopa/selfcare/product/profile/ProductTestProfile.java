package it.pagopa.selfcare.product.profile;

import io.quarkus.test.junit.QuarkusTestProfile;
import java.util.Map;

public class ProductTestProfile implements QuarkusTestProfile {
  @Override
  public Map<String, String> getConfigOverrides() {
    return Map.of("product-cdc.mongodb.watch.enabled", "false");
  }
}
