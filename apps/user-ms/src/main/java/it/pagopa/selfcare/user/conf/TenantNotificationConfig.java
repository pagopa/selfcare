package it.pagopa.selfcare.user.conf;

import io.quarkus.runtime.Startup;
import it.pagopa.selfcare.tenant.TenantContext;
import it.pagopa.selfcare.tenant.TenantRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.eclipse.microprofile.config.Config;

@Startup
@ApplicationScoped
public class TenantNotificationConfig {
  @Inject Config config;
  @Inject TenantRegistry registry;
  @Inject TenantContext context;
  private Map<String, Settings> settings;

  @PostConstruct
  void initialize() {
    Map<String, Settings> values = new LinkedHashMap<>();
    for (String tenant : registry.supportedTenantIds()) {
      String prefix = "tenant." + tenant.toLowerCase(Locale.ROOT) + ".";
      boolean mailEnabled = flag(prefix + "mail.enabled");
      String apiKey = value(prefix + "one-mail.api-key");
      if (mailEnabled && apiKey.isBlank()) {
        throw new IllegalStateException("Missing OneMail API key for tenant " + tenant);
      }
      boolean usersEnabled = flag(prefix + "eventhub.users.enabled");
      if (usersEnabled && !"AR".equals(tenant)) {
        throw new IllegalStateException("Event Hub is configured only for AR");
      }
      Map<String, String> templates = new LinkedHashMap<>();
      for (String name : List.of("activated", "deleted", "suspended", "request", "convention", "multi-role", "single-role")) {
        templates.put(name, required(prefix + "onemail.template." + name));
      }
      boolean eventsEnabled = flag(prefix + "internalevents.enabled");
      String connection = value(prefix + "internalevents.connection-string");
      String namespace = value(prefix + "internalevents.namespace");
      String clientId = value(prefix + "internalevents.client-id");
      if (eventsEnabled && connection.isBlank() && (namespace.isBlank() || clientId.isBlank())) {
        throw new IllegalStateException("Missing internal events configuration for tenant " + tenant);
      }
      values.put(tenant, new Settings(required(prefix + "selfcare-url"), mailEnabled,
          required(prefix + "mail-sender"), apiKey, Map.copyOf(templates),
          flag(prefix + "webhook.enabled"), usersEnabled,
          new InternalEventsSettings(eventsEnabled, namespace, clientId, connection)));
    }
    settings = Map.copyOf(values);
  }

  public Settings current() {
    return forTenant(context.requiredTenantId());
  }

  public Settings forTenant(String tenant) {
    registry.resolve(tenant);
    return settings.get(registry.normalizeTenantId(tenant));
  }

  private boolean flag(String key) {
    return config.getOptionalValue(key, Boolean.class).orElse(false);
  }

  private String value(String key) {
    return config.getOptionalValue(key, String.class).filter(v -> !v.isBlank()).orElse("");
  }

  private String required(String key) {
    String result = value(key);
    if (result.isBlank()) {
      throw new IllegalStateException("Missing configuration: " + key);
    }
    return result;
  }

  public record Settings(String selfcareUrl, boolean mailEnabled, String sender, String apiKey,
                         Map<String, String> templates, boolean webhookEnabled, boolean usersEnabled,
                         InternalEventsSettings internalEvents) {
    @Override public String toString() { return "TenantNotificationSettings[REDACTED]"; }
  }

  public record InternalEventsSettings(boolean enabled, String namespace, String clientId,
                                       String connectionString) {
    @Override public String toString() { return "InternalEventsSettings[REDACTED]"; }
  }
}
