package it.pagopa.selfcare.user.conf;

import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.tenant.TenantContext;
import it.pagopa.selfcare.tenant.TenantRegistry;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@QuarkusTest
class TenantNotificationConfigTest {
  @Inject TenantNotificationConfig config;
  @Inject TenantContext context;
  @Inject TenantRegistry registry;

  @Test void settingsAreIsolatedAndPreservePnpgTemplates() {
    context.setTenantId("AR");
    assertTrue(config.current().mailEnabled());
    assertTrue(config.current().usersEnabled());
    assertTrue(config.current().webhookEnabled());
    assertEquals("selfcare_user_activated", config.current().templates().get("activated"));
    context.setTenantId("PNPG");
    assertFalse(config.current().usersEnabled());
    assertFalse(config.current().webhookEnabled());
    assertEquals("selfcare_pnpg_user_activated", config.current().templates().get("activated"));
    assertNotEquals(config.forTenant("AR").apiKey(), config.current().apiKey());
    assertEquals("TenantNotificationSettings[REDACTED]", config.current().toString());
  }

  @Test void startupRejectsEnabledMailWithoutCredentials() {
    TenantNotificationConfig invalid = new TenantNotificationConfig();
    invalid.registry = registry;
    invalid.config = spy(ConfigProvider.getConfig());
    doReturn(java.util.Optional.empty()).when(invalid.config).getOptionalValue("tenant.ar.one-mail.api-key", String.class);
    assertThrows(IllegalStateException.class, invalid::initialize);
  }

  @Test void disabledMailDoesNotRequireCredentials() {
    TenantNotificationConfig disabled = new TenantNotificationConfig();
    disabled.registry = registry;
    Config values = spy(ConfigProvider.getConfig());
    doReturn(java.util.Optional.of(false)).when(values).getOptionalValue("tenant.ar.mail.enabled", Boolean.class);
    doReturn(java.util.Optional.empty()).when(values).getOptionalValue("tenant.ar.one-mail.api-key", String.class);
    disabled.config = values;
    assertDoesNotThrow(disabled::initialize);
  }
}
