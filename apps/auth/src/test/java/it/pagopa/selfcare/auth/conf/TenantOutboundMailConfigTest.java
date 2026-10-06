package it.pagopa.selfcare.auth.conf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.Test;

class TenantOutboundMailConfigTest {

  @Test
  void requiresMailSettingsOnlyForEnabledTenants() {
    TenantRegistry registry = mock(TenantRegistry.class);
    when(registry.enabledAuthenticationTenants())
        .thenReturn(List.of(new TenantRegistry.Tenant("AR", null)));
    Config config = mock(Config.class);
    when(config.getOptionalValue("tenant.ar.mail-sender", String.class))
        .thenReturn(Optional.of("noreply@selfcare.pagopa.it"));
    when(config.getOptionalValue("tenant.ar.one-mail.api-key", String.class))
        .thenReturn(Optional.of("ar-key"));
    TenantOutboundMailConfig mailConfig = new TenantOutboundMailConfig();
    mailConfig.tenantRegistry = registry;
    mailConfig.config = config;
    mailConfig.initialize();

    assertEquals("ar-key", mailConfig.apiKey("AR"));
    assertEquals("noreply@selfcare.pagopa.it", mailConfig.sender("AR"));
    assertThrows(IllegalStateException.class, () -> mailConfig.apiKey("PNPG"));
  }

  @Test
  void rejectsMissingCredentialForEnabledTenant() {
    TenantRegistry registry = mock(TenantRegistry.class);
    when(registry.enabledAuthenticationTenants())
        .thenReturn(List.of(new TenantRegistry.Tenant("AR", null)));
    Config config = mock(Config.class);
    when(config.getOptionalValue("tenant.ar.mail-sender", String.class))
        .thenReturn(Optional.of("noreply@selfcare.pagopa.it"));
    when(config.getOptionalValue("tenant.ar.one-mail.api-key", String.class))
        .thenReturn(Optional.of(" "));
    TenantOutboundMailConfig mailConfig = new TenantOutboundMailConfig();
    mailConfig.tenantRegistry = registry;
    mailConfig.config = config;

    assertThrows(IllegalStateException.class, mailConfig::initialize);
  }

  @Test
  void resolvesSenderAndApiKeyIndependentlyForEachEnabledTenant() {
    TenantRegistry registry = mock(TenantRegistry.class);
    when(registry.enabledAuthenticationTenants())
        .thenReturn(
            List.of(
                new TenantRegistry.Tenant("AR", null),
                new TenantRegistry.Tenant("PNPG", null)));
    Config config = mock(Config.class);
    when(config.getOptionalValue("tenant.ar.mail-sender", String.class))
        .thenReturn(Optional.of("ar@example.test"));
    when(config.getOptionalValue("tenant.ar.one-mail.api-key", String.class))
        .thenReturn(Optional.of("ar-key"));
    when(config.getOptionalValue("tenant.pnpg.mail-sender", String.class))
        .thenReturn(Optional.of("pnpg@example.test"));
    when(config.getOptionalValue("tenant.pnpg.one-mail.api-key", String.class))
        .thenReturn(Optional.of("pnpg-key"));
    TenantOutboundMailConfig mailConfig = new TenantOutboundMailConfig();
    mailConfig.tenantRegistry = registry;
    mailConfig.config = config;

    mailConfig.initialize();

    assertEquals("ar@example.test", mailConfig.sender("AR"));
    assertEquals("ar-key", mailConfig.apiKey("AR"));
    assertEquals("pnpg@example.test", mailConfig.sender("PNPG"));
    assertEquals("pnpg-key", mailConfig.apiKey("PNPG"));
  }

  @Test
  void rejectsMissingSenderForEnabledTenant() {
    TenantRegistry registry = mock(TenantRegistry.class);
    when(registry.enabledAuthenticationTenants())
        .thenReturn(List.of(new TenantRegistry.Tenant("AR", null)));
    Config config = mock(Config.class);
    when(config.getOptionalValue("tenant.ar.mail-sender", String.class))
        .thenReturn(Optional.empty());
    TenantOutboundMailConfig mailConfig = new TenantOutboundMailConfig();
    mailConfig.tenantRegistry = registry;
    mailConfig.config = config;

    assertThrows(IllegalStateException.class, mailConfig::initialize);
  }
}
