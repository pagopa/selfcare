package it.pagopa.selfcare.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.auth.conf.TenantRegistry;
import it.pagopa.selfcare.auth.conf.TenantSessionKeyProvider;
import it.pagopa.selfcare.auth.exception.InternalException;
import it.pagopa.selfcare.auth.model.UserClaims;
import jakarta.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class SessionServiceTest {

  @Inject SessionService sessionService;
  @Inject JWTParser jwtParser;
  @Inject ObjectMapper objectMapper;
  @Inject TenantRegistry tenantRegistry;
  @Inject TenantSessionKeyProvider tenantSessionKeyProvider;

  @Test
  void generateSessionTokenWithValidInputs() throws Exception {
    String token =
        sessionService
            .generateSessionToken(
                UserClaims.builder()
                    .uid("uid")
                    .fiscalCode("fiscalNumber")
                    .name("name")
                    .familyName("familyName")
                    .tenantId("AR")
                    .build())
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create())
            .assertCompleted()
            .getItem();

    assertEquals("AR", jwtParser.parseOnly(token).getClaim("tenant_id"));
    assertEquals("jwt_test_kid", tokenHeader(token).get("kid").asText());
    assertEquals(
        Set.of("api.dev.selfcare.pagopa.it"), jwtParser.parseOnly(token).getAudience());
  }

  @Test
  void generateSessionTokenWithTenantAudienceWhenEnabled() throws Exception {
    SessionServiceImpl service = sessionService(true, "global.audience");

    String token =
        service
            .generateSessionToken(fullClaims())
            .await()
            .indefinitely();
    String internalToken =
        service
            .generateSessionTokenInternal(fullClaims())
            .await()
            .indefinitely();

    assertEquals(
        Set.of("api.test-tenant.selfcare.pagopa.it"), jwtParser.parseOnly(token).getAudience());
    assertEquals(
        Set.of("api.test-tenant.selfcare.pagopa.it"),
        jwtParser.parseOnly(internalToken).getAudience());
  }

  @Test
  void generateSessionTokenWithGlobalAudienceWhenTenantAudienceDisabled() throws Exception {
    String token =
        sessionService(false, "global.audience")
            .generateSessionToken(fullClaims())
            .await()
            .indefinitely();

    assertEquals(Set.of("global.audience"), jwtParser.parseOnly(token).getAudience());
  }

  @Test
  void generateInternalSessionTokenWithTenantSigningKey() throws Exception {
    String token =
        sessionService
            .generateSessionTokenInternal(
                UserClaims.builder().uid("uid").email("email").tenantId("AR").build())
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create())
            .assertCompleted()
            .getItem();

    assertEquals("AR", jwtParser.parseOnly(token).getClaim("tenant_id"));
    assertEquals("jwt_test_kid", tokenHeader(token).get("kid").asText());
  }

  @Test
  void rejectSessionTokenForTenantWithoutSigningKey() {
    UserClaims userClaims = UserClaims.builder().uid("uid").tenantId("PNPG").build();

    assertThrows(InternalException.class, () -> sessionService.generateSessionToken(userClaims));
  }

  private SessionServiceImpl sessionService(boolean tenantAudienceEnabled, String audience) {
    SessionServiceImpl service = new SessionServiceImpl();
    service.sessionDuration = 1;
    service.audience = audience;
    service.tenantAudienceEnabled = tenantAudienceEnabled;
    service.tenantRegistry = tenantRegistry;
    service.tenantSessionKeyProvider = tenantSessionKeyProvider;
    return service;
  }

  private UserClaims fullClaims() {
    return UserClaims.builder()
        .uid("uid")
        .email("email")
        .fiscalCode("fiscalNumber")
        .name("name")
        .familyName("familyName")
        .tenantId("AR")
        .build();
  }

  private JsonNode tokenHeader(String token) throws Exception {
    byte[] decodedHeader = Base64.getUrlDecoder().decode(token.split("\\.")[0]);
    return objectMapper.readTree(new String(decodedHeader, StandardCharsets.UTF_8));
  }
}
