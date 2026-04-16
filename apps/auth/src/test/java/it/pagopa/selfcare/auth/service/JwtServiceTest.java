package it.pagopa.selfcare.auth.service;

import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import jakarta.inject.Inject;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class JwtServiceTest {

  private static final String JWT =
      "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTAyMn0.KMUFsIDTnFmyG3nMiGM6H9FNFUROf3wh7SmqJp-QV30";
  @Inject JwtService jwtService;

  @Test
  void extractJwtClaimsWithValidJwt() {
    Map<String, String> jwtClaims =
        jwtService
            .extractClaimsFromJwtToken(JWT)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create())
            .assertCompleted()
            .getItem();
    Assertions.assertTrue(jwtClaims.containsKey("name"));
    Assertions.assertEquals("John Doe", jwtClaims.get("name"));
    Assertions.assertTrue(jwtClaims.containsKey("admin"));
    Assertions.assertEquals("true", jwtClaims.get("admin"));
  }

  @Test
  void failExtractingJwtClaimsWithInvalidJwt() {
    jwtService
        .extractClaimsFromJwtToken("")
        .subscribe()
        .withSubscriber(UniAssertSubscriber.create())
        .assertFailed()
        .assertFailedWith(Exception.class, "Parsed jwt is null");
  }
}
