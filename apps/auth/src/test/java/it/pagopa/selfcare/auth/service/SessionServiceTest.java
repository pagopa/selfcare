package it.pagopa.selfcare.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.auth.model.UserClaims;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class SessionServiceTest {

  @Inject SessionService sessionService;
  @Inject JWTParser jwtParser;

  @Test
  void generateSessionTokenWithValidInputs() throws ParseException {
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
  }
}
