package it.pagopa.selfcare.auth.conf;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import java.util.Optional;

@ConfigMapping(prefix = "auth.m2m")
public interface MachineTokenConfig {
  @WithDefault("false")
  boolean enabled();

  @WithDefault("8082")
  int port();

  @WithDefault("true")
  boolean tlsEnabled();

  Optional<String> certificatePem();

  Optional<String> privateKeyPem();
}
