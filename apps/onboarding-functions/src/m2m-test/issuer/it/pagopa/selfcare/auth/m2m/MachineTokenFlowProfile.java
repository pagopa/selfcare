package it.pagopa.selfcare.auth.m2m;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTestProfile;
import java.security.KeyPairGenerator;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MachineTokenFlowProfile implements QuarkusTestProfile {
  static final Map<String, String> IDENTITIES = Map.of(
      "onboarding-ms", "11111111-1111-4111-8111-111111111111",
      "onboarding-cdc", "22222222-2222-4222-8222-222222222222");

  @Override
  public Map<String, String> getConfigOverrides() {
    try {
      Map<String, String> config = new HashMap<>();
      Map<String, Object> tenants = new HashMap<>();
      List<Map<String, String>> grants = new ArrayList<>();
      var generator = KeyPairGenerator.getInstance("RSA");
      generator.initialize(2048);
      for (String tenant : List.of("AR", "PNPG")) {
        var keys = generator.generateKeyPair();
        String prefix = "M2M_TEST_" + tenant;
        config.put(prefix + "_PRIVATE_KEY", "-----BEGIN PRIVATE KEY-----\n"
            + Base64.getEncoder().encodeToString(keys.getPrivate().getEncoded())
            + "\n-----END PRIVATE KEY-----");
        config.put(prefix + "_PUBLIC_KEY", Base64.getEncoder().encodeToString(keys.getPublic().getEncoded()));
        config.put(prefix + "_KEY_ID", tenant + "-test-key");
        config.put(prefix + "_MONGO", "mongodb://127.0.0.1:27017");
        tenants.put(tenant, Map.of(
            "auth_enabled", true,
            "authentication_provider", "HUB_SPID_LOGIN",
            "frontend_uri", "https://frontend.invalid",
            "api_uri", "https://api.invalid",
            "allowed_origins", List.of("https://frontend.invalid"),
            "mongo", Map.of("account", "test-" + tenant, "database", "selcAuth",
                "connectionStringEnvVar", prefix + "_MONGO"),
            "jwt", Map.of("publicKeyEnvVar", prefix + "_PUBLIC_KEY",
                "session", Map.of("privateKeyEnvVar", prefix + "_PRIVATE_KEY",
                    "keyIdEnvVar", prefix + "_KEY_ID", "audience", "api.invalid"))));
        IDENTITIES.forEach((service, oid) -> grants.add(Map.of(
            "objectId", oid, "serviceId", service, "tenantId", tenant,
            "audience", "onboarding-functions", "scope", "onboarding-functions.invoke")));
      }
      ObjectMapper mapper = new ObjectMapper();
      config.put("tenant.registry.json", mapper.writeValueAsString(tenants));
      config.put("auth.m2m.grants", mapper.writeValueAsString(grants));
      config.put("auth.m2m.enabled", "true");
      config.put("auth.m2m.lifetime", "PT5M");
      config.put("auth.m2m.entra-tenant-id", "33333333-3333-4333-8333-333333333333");
      config.put("auth.m2m.entra-audience", "api://auth");
      config.put("quarkus.http.test-port", "0");
      config.put("quarkus.mongodb.devservices.enabled", "false");
      config.put("quarkus.arc.exclude-types", "it.pagopa.selfcare.onboarding.service.HttpJwtValidator");
      return config;
    } catch (Exception exception) {
      throw new IllegalStateException("Cannot configure the M2M integration fixture", exception);
    }
  }
}
