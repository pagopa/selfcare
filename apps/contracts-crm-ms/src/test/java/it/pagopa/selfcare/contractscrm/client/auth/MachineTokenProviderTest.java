package it.pagopa.selfcare.contractscrm.client.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.contractscrm.config.ContractsCrmConfig;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MachineTokenProviderTest {

  private ContractsCrmConfig config;
  private ContractsCrmConfig.MachineToken tokenConfig;

  @BeforeEach
  void setUp() throws Exception {
    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    String privateKey =
        Base64.getEncoder().encodeToString(generator.generateKeyPair().getPrivate().getEncoded());
    config = mock(ContractsCrmConfig.class);
    tokenConfig = mock(ContractsCrmConfig.MachineToken.class);
    when(config.tenantId()).thenReturn("AR");
    when(config.machineToken()).thenReturn(tokenConfig);
    when(tokenConfig.issuer()).thenReturn("SPID");
    when(tokenConfig.uid()).thenReturn("contracts-crm-ms");
    when(tokenConfig.duration()).thenReturn(Duration.ofHours(1));
    when(tokenConfig.privateKey()).thenReturn(Optional.of(privateKey));
    when(tokenConfig.kid()).thenReturn(Optional.of("kid-1"));
  }

  @Test
  void getToken_containsClaimsRequiredByOnboardingMs() throws Exception {
    String token = new MachineTokenProvider(config).getToken();

    String[] parts = token.split("\\.");
    JsonNode header = decode(parts[0]);
    JsonNode claims = decode(parts[1]);
    assertEquals("kid-1", header.get("kid").asText());
    assertEquals("RS256", header.get("alg").asText());
    assertEquals("SPID", claims.get("iss").asText());
    assertEquals("contracts-crm-ms", claims.get("uid").asText());
    assertEquals("contracts-crm-ms", claims.get("sub").asText());
    assertEquals("AR", claims.get(MachineTokenProvider.TENANT_CLAIM).asText());
    assertTrue(claims.get("exp").asLong() > claims.get("iat").asLong());
  }

  @Test
  void getToken_isCachedUntilExpiry() {
    MachineTokenProvider provider = new MachineTokenProvider(config);

    assertSame(provider.getToken(), provider.getToken());
  }

  @Test
  void getToken_withoutPrivateKey_fails() {
    when(tokenConfig.privateKey()).thenReturn(Optional.empty());
    MachineTokenProvider provider = new MachineTokenProvider(config);

    assertThrows(IllegalStateException.class, provider::getToken);
  }

  @Test
  void onboardingHeadersFactory_addsBearerTokenAndTenant() {
    MachineTokenProvider provider = mock(MachineTokenProvider.class);
    when(provider.getToken()).thenReturn("token");
    MultivaluedMap<String, String> headers =
        new OnboardingHeadersFactory(provider, config)
            .update(new MultivaluedHashMap<>(), new MultivaluedHashMap<>());

    assertEquals("Bearer token", headers.getFirst(OnboardingHeadersFactory.AUTHORIZATION_HEADER));
    assertEquals("AR", headers.getFirst(OnboardingHeadersFactory.TENANT_HEADER));
  }

  private static JsonNode decode(String part) throws Exception {
    return new ObjectMapper()
        .readTree(new String(Base64.getUrlDecoder().decode(part), StandardCharsets.UTF_8));
  }
}
