package it.pagopa.selfcare.auth.conf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class TenantDefinitionTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void deserializeTenantWithSessionAudience() throws Exception {
    TenantDefinition definition =
        objectMapper.readValue(
            """
            {
              "frontend_uri": "https://imprese.notifichedigitali.it",
              "api_uri": "https://api-pnpg.selfcare.pagopa.it",
              "allowed_origins": ["https://imprese.notifichedigitali.it"],
              "authentication_provider": "HUB_SPID_LOGIN",
              "auth_enabled": true,
              "mongo": {"account": "cosmos"},
              "jwt": {
                "publicKeyEnvVar": "JWT_PUBLIC_KEY_PNPG",
                "session": {
                  "privateKeyEnvVar": "TENANT_PNPG_JWT_SESSION_PRIVATE_KEY",
                  "keyIdEnvVar": "TENANT_PNPG_JWT_SESSION_KEY_ID",
                  "audience": "imprese.notifichedigitali.it",
                  "unknown": "ignored"
                }
              }
            }
            """,
            TenantDefinition.class);

    assertEquals("https://imprese.notifichedigitali.it", definition.frontendUri());
    assertEquals("https://api-pnpg.selfcare.pagopa.it", definition.apiUri());
    assertEquals(List.of("https://imprese.notifichedigitali.it"), definition.allowedOrigins());
    assertEquals("HUB_SPID_LOGIN", definition.authenticationProvider());
    assertTrue(definition.authEnabled());
    assertEquals("JWT_PUBLIC_KEY_PNPG", definition.jwt().publicKeyEnvVar());
    TenantDefinition.SessionDefinition session = definition.jwt().session();
    assertEquals("TENANT_PNPG_JWT_SESSION_PRIVATE_KEY", session.privateKeyEnvVar());
    assertEquals("TENANT_PNPG_JWT_SESSION_KEY_ID", session.keyIdEnvVar());
    assertEquals("imprese.notifichedigitali.it", session.audience());
  }

  @Test
  void deserializeTenantWithoutSessionAudience() throws Exception {
    TenantDefinition definition =
        objectMapper.readValue(
            """
            {
              "auth_enabled": true,
              "jwt": {
                "session": {
                  "privateKeyEnvVar": "TENANT_AR_JWT_SESSION_PRIVATE_KEY",
                  "keyIdEnvVar": "TENANT_AR_JWT_SESSION_KEY_ID"
                }
              }
            }
            """,
            TenantDefinition.class);

    assertEquals("TENANT_AR_JWT_SESSION_KEY_ID", definition.jwt().session().keyIdEnvVar());
    assertNull(definition.jwt().session().audience());
  }

  @Test
  void deserializeTenantWithoutJwt() throws Exception {
    TenantDefinition definition =
        objectMapper.readValue("{\"auth_enabled\": false}", TenantDefinition.class);

    assertFalse(definition.authEnabled());
    assertNull(definition.jwt());
  }

  @Test
  void tenantDefinitionShortConstructorHasNoJwt() {
    TenantDefinition definition =
        new TenantDefinition(
            "https://selfcare.pagopa.it",
            "https://api.selfcare.pagopa.it",
            List.of("https://selfcare.pagopa.it"),
            TenantRegistry.ONE_IDENTITY,
            true);

    assertEquals(TenantRegistry.ONE_IDENTITY, definition.authenticationProvider());
    assertNull(definition.jwt());
  }
}
