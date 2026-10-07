package it.pagopa.selfcare.auth.conf;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.eclipse.microprofile.config.Config;

@ApplicationScoped
public class TenantOutboundMailConfig {

  @Inject TenantRegistry tenantRegistry;
  @Inject Config config;

  private Map<String, MailSettings> settings;

  @PostConstruct
  void initialize() {
    settings =
        tenantRegistry.enabledAuthenticationTenants().stream()
            .collect(
                Collectors.toUnmodifiableMap(
                    TenantRegistry.Tenant::id,
                    tenant -> {
                      String prefix = "tenant." + tenant.id().toLowerCase(Locale.ROOT) + ".";
                      return new MailSettings(
                          required(prefix + "mail-sender", tenant.id()),
                          required(prefix + "one-mail.api-key", tenant.id()));
                    }));
  }

  public String sender(String tenantId) {
    return settings(tenantId).sender();
  }

  public String apiKey(String tenantId) {
    return settings(tenantId).apiKey();
  }

  private MailSettings settings(String tenantId) {
    MailSettings value = settings.get(tenantId);
    if (value == null) {
      throw new IllegalStateException("Mail is not configured for tenant " + tenantId);
    }
    return value;
  }

  private String required(String property, String tenantId) {
    return config
        .getOptionalValue(property, String.class)
        .filter(value -> !value.isBlank())
        .orElseThrow(() -> new IllegalStateException("Missing mail configuration for tenant " + tenantId));
  }

  private record MailSettings(String sender, String apiKey) {
    @Override
    public String toString() {
      return "MailSettings[REDACTED]";
    }
  }
}
