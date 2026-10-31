package it.pagopa.selfcare.auth.conf;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import java.time.Duration;
import java.util.Optional;

@ConfigMapping(prefix = "auth.m2m")
public interface MachineTokenConfig {
  @WithDefault("false")
  boolean enabled();

  Optional<String> entraTenantId();

  Optional<String> entraAudience();

  @WithDefault("PT5M")
  Duration lifetime();

  @WithDefault("[]")
  String grants();
}
