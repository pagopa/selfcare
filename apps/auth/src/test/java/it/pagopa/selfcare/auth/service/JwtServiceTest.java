package it.pagopa.selfcare.auth.service;

import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class JwtServiceTest {

  @Inject JwtService jwtService;

  private static final String JWT = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTAyMn0.KMUFsIDTnFmyG3nMiGM6H9FNFUROf3wh7SmqJp-QV30";

  @Test
  void extractJwtClaimsWithValidJwt() {
    jwtService
        .extractClaimsFromJwtToken(JWT)
        .subscribe()
        .withSubscriber(UniAssertSubscriber.create())
        .assertCompleted();
  }
}
