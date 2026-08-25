package it.pagopa.selfcare.auth.conf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
        .thenReturn(List.of(new TenantRegistry.Tenant("AR", mock(TenantDefinition.class))));

    provider = new TenantSessionKeyProvider();
    provider.tenantRegistry = tenantRegistry;
    provider.config = config;
  }

  @Test
  void loadSigningKeysForEveryEnabledTenant() {
    when(config.getOptionalValue("tenant.ar.jwt.session.private-key", String.class))
        .thenReturn(Optional.of(privateKeyPem));
    when(config.getOptionalValue("tenant.ar.jwt.session.key-id", String.class))
        .thenReturn(Optional.of("ar-kid"));

    provider.initialize();

    assertNotNull(provider.getSigningKey("AR").privateKey());
    assertEquals("ar-kid", provider.getSigningKey("AR").keyId());
  }

  @Test
  void failStartupWhenSigningKeyIsMissing() {
    when(config.getOptionalValue("tenant.ar.jwt.session.private-key", String.class))
        .thenReturn(Optional.empty());

    assertThrows(IllegalStateException.class, provider::initialize);
  }

  @Test
  void failStartupWhenSigningKeyIsInvalid() {
    when(config.getOptionalValue("tenant.ar.jwt.session.private-key", String.class))
        .thenReturn(Optional.of("invalid"));
    when(config.getOptionalValue("tenant.ar.jwt.session.key-id", String.class))
        .thenReturn(Optional.of("ar-kid"));

    assertThrows(IllegalStateException.class, provider::initialize);
  }
}
