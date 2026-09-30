package it.pagopa.selfcare.product.testsupport;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.mongodb.MongoTestResource;
import java.util.HashMap;
import java.util.Map;

public class TenantMongoTestResource implements QuarkusTestResourceLifecycleManager {

  private final MongoTestResource delegate = new MongoTestResource();

  @Override
  public void init(Map<String, String> initArgs) {
    delegate.init(initArgs);
  }

  @Override
  public Map<String, String> start() {
    Map<String, String> conf = new HashMap<>(delegate.start());
    String connectionString = conf.get("quarkus.mongodb.connection-string");
    if (connectionString != null) {
      conf.put("MONGODB_CONNECTION_STRING_AR", connectionString);
      conf.put("MONGODB_CONNECTION_STRING_PNPG", connectionString);
    }
    return conf;
  }

  @Override
  public void stop() {
    delegate.stop();
  }
}
