package it.pagopa.selfcare.onboarding.support;

import io.quarkus.test.junit.QuarkusTestProfile;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

public class ProductHttpContractProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("certs/pk-key.pub")) {
            if (Objects.isNull(input)) {
                throw new IllegalStateException("Missing test JWT public key");
            }
            return Map.of(
                    "mp.jwt.verify.publickey", new String(input.readAllBytes(), StandardCharsets.UTF_8),
                    "tenant.enforcement.enabled", "true",
                    "quarkus.mongodb.devservices.enabled", "false");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
