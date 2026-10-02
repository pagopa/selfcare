package it.pagopa.selfcare.auth.conf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.pagopa.selfcare.auth.exception.InternalException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TenantSessionKeyProviderTest {

  @Mock TenantRegistry tenantRegistry;
  @Mock Config config;

  private TenantSessionKeyProvider provider;
  private String privateKeyPem;

  @BeforeEach
  void setUp() throws Exception {
    Properties properties = new Properties();
    try (InputStream input = getClass().getResourceAsStream("/application.properties")) {
      properties.load(input);
    }
    privateKeyPem = properties.getProperty("jwt.session.private.key");

    when(tenantRegistry.enabledAuthenticationTenants())
        .thenReturn(List.of(tenantWithSigningReferences("AR")));

    provider = new TenantSessionKeyProvider();
    provider.tenantRegistry = tenantRegistry;
    provider.config = config;
  }

  @Test
  void loadSigningKeysForEveryEnabledTenant() {
    when(config.getOptionalValue("TENANT_AR_JWT_SESSION_PRIVATE_KEY", String.class))
        .thenReturn(Optional.of(privateKeyPem));
    when(config.getOptionalValue("TENANT_AR_JWT_SESSION_KEY_ID", String.class))
        .thenReturn(Optional.of("ar-kid"));

    provider.initialize();

    assertNotNull(provider.getSigningKey("AR").privateKey());
    assertEquals("ar-kid", provider.getSigningKey("AR").keyId());
  }

  @Test
  void failStartupWhenSigningKeyIsMissing() {
    when(config.getOptionalValue("TENANT_AR_JWT_SESSION_PRIVATE_KEY", String.class))
        .thenReturn(Optional.empty());

    assertThrows(IllegalStateException.class, provider::initialize);
  }

  @Test
  void failStartupWhenSigningKeyIsInvalid() {
    when(config.getOptionalValue("TENANT_AR_JWT_SESSION_PRIVATE_KEY", String.class))
        .thenReturn(Optional.of("invalid"));
    when(config.getOptionalValue("TENANT_AR_JWT_SESSION_KEY_ID", String.class))
        .thenReturn(Optional.of("ar-kid"));

    assertThrows(IllegalStateException.class, provider::initialize);
  }

  @Test
  void failStartupWhenSigningKeyIsBlank() {
    when(config.getOptionalValue("TENANT_AR_JWT_SESSION_PRIVATE_KEY", String.class))
        .thenReturn(Optional.of(" "));

    assertThrows(IllegalStateException.class, provider::initialize);
  }

  @Test
  void failStartupWhenKeyIdIsMissing() {
    when(config.getOptionalValue("TENANT_AR_JWT_SESSION_PRIVATE_KEY", String.class))
        .thenReturn(Optional.of(privateKeyPem));
    when(config.getOptionalValue("TENANT_AR_JWT_SESSION_KEY_ID", String.class))
        .thenReturn(Optional.empty());

    assertThrows(IllegalStateException.class, provider::initialize);
  }

  @Test
  void failStartupWhenKeyIdIsBlank() {
    when(config.getOptionalValue("TENANT_AR_JWT_SESSION_PRIVATE_KEY", String.class))
        .thenReturn(Optional.of(privateKeyPem));
    when(config.getOptionalValue("TENANT_AR_JWT_SESSION_KEY_ID", String.class))
        .thenReturn(Optional.of(" "));

    assertThrows(IllegalStateException.class, provider::initialize);
  }

  @Test
  void getSigningKeyRejectsTenantWithoutConfiguredKey() {
    when(config.getOptionalValue("TENANT_AR_JWT_SESSION_PRIVATE_KEY", String.class))
        .thenReturn(Optional.of(privateKeyPem));
    when(config.getOptionalValue("TENANT_AR_JWT_SESSION_KEY_ID", String.class))
        .thenReturn(Optional.of("ar-kid"));
    provider.initialize();

    assertThrows(InternalException.class, () -> provider.getSigningKey("PNPG"));
  }

  @Test
  void loadSigningKeysWithoutCrossTenantFallback() {
    when(tenantRegistry.enabledAuthenticationTenants())
        .thenReturn(
            List.of(
                tenantWithSigningReferences("AR"), tenantWithSigningReferences("PNPG")));
    when(config.getOptionalValue("TENANT_AR_JWT_SESSION_PRIVATE_KEY", String.class))
        .thenReturn(Optional.of(privateKeyPem));
    when(config.getOptionalValue("TENANT_AR_JWT_SESSION_KEY_ID", String.class))
        .thenReturn(Optional.of("ar-kid"));
    when(config.getOptionalValue("TENANT_PNPG_JWT_SESSION_PRIVATE_KEY", String.class))
        .thenReturn(Optional.of(privateKeyPem));
    when(config.getOptionalValue("TENANT_PNPG_JWT_SESSION_KEY_ID", String.class))
        .thenReturn(Optional.of("pnpg-kid"));

    provider.initialize();

    assertEquals("ar-kid", provider.getSigningKey("AR").keyId());
    assertEquals("pnpg-kid", provider.getSigningKey("PNPG").keyId());
    verify(config).getOptionalValue("TENANT_AR_JWT_SESSION_PRIVATE_KEY", String.class);
    verify(config).getOptionalValue("TENANT_PNPG_JWT_SESSION_PRIVATE_KEY", String.class);
  }

  @Test
  void failStartupWhenTenantRegistryDoesNotConfigureSessionSigningReferences() {
    when(tenantRegistry.enabledAuthenticationTenants())
        .thenReturn(List.of(tenantWithoutSigningReferences("AR")));

    assertThrows(IllegalStateException.class, provider::initialize);
  }

  @Test
  void initializeSupportsNoEnabledAuthenticationTenants() {
    when(tenantRegistry.enabledAuthenticationTenants()).thenReturn(List.of());

    provider.initialize();

    assertThrows(InternalException.class, () -> provider.getSigningKey("AR"));
  }

  private TenantRegistry.Tenant tenantWithSigningReferences(String tenantId) {
    return new TenantRegistry.Tenant(
        tenantId,
        new TenantDefinition(
            null,
            null,
            List.of(),
            TenantRegistry.ONE_IDENTITY,
            true,
            new TenantDefinition.JwtDefinition(
                null,
                new TenantDefinition.SessionDefinition(
                    "TENANT_" + tenantId + "_JWT_SESSION_PRIVATE_KEY",
                    "TENANT_" + tenantId + "_JWT_SESSION_KEY_ID"))));
  }

  private TenantRegistry.Tenant tenantWithoutSigningReferences(String tenantId) {
    return new TenantRegistry.Tenant(
        tenantId,
        new TenantDefinition(null, null, List.of(), TenantRegistry.ONE_IDENTITY, true));
  }
}
