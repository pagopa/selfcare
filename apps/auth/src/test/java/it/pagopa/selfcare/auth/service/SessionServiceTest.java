package it.pagopa.selfcare.auth.service;

import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.auth.model.error.UserClaims;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class SessionServiceTest {

  @Inject SessionService sessionService;

  @Test
  void generateSessionTokenWithValidInputs() {
    sessionService
        .generateSessionToken(
            UserClaims.builder()
                .uid("uid")
                .fiscalCode("fiscalNumber")
                .name("name")
                .familyName("familyName")
                .build())
        .subscribe()
        .withSubscriber(UniAssertSubscriber.create())
        .assertCompleted();
  }
}
