package it.pagopa.selfcare.auth.service;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.jwt.auth.principal.ParseException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import jakarta.inject.Inject;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openapi.quarkus.one_identity_json.api.TokenServerApisApi;
import org.openapi.quarkus.one_identity_json.model.TokenData;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@QuarkusTest
public class OidcServiceTest {

  private static final String SESSION_TOKEN =
      "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJTZWxmY2FyZSBBdXRoIiwiaWF0IjoxNzQzNTIyMzIyLCJleHAiOjE3NDM1MjIzMjQsImF1ZCI6Ind3dy5leGFtcGxlLmNvbSIsInN1YiI6Impyb2NrZXRAZXhhbXBsZS5jb20iLCJuYW1lIjoiSm9obm55IiwiZmFtaWx5X25hbWUiOiJSb2NrZXQiLCJmaXNjYWxfbnVtYmVyIjoiVElOSVQtRklTQ0FMQ09ERSJ9.SVN74DC8_KHqxJsHO5vwAcbbTf9YEJyaKmYfdVZjTSY";
  @Inject OidcService oidcService;
  @InjectMock SessionService sessionService;
  @InjectMock JwtService jwtService;

  @RestClient @InjectMock TokenServerApisApi tokenApi;

  @Test
  void exchangeAuthCodeWithSessionToken() throws ParseException {

    when(tokenApi.createRequestToken(any(), anyString()))
        .thenReturn(Uni.createFrom().item(TokenData.builder().idToken("idToken").build()));
    when(jwtService.extractClaimsFromJwtToken(anyString()))
        .thenReturn(Uni.createFrom().item(Map.of("fiscal_number", "TINIT-FISCALCODE", "name", "name", "family_name", "Doe")));

    when(sessionService.generateSessionToken(anyString(), anyString(), anyString()))
        .thenReturn(Uni.createFrom().item(SESSION_TOKEN));

    var result = oidcService
        .exchange("authCode", "redirectUri")
        .subscribe()
        .withSubscriber(UniAssertSubscriber.create())
        .assertCompleted().getItem();

    Assertions.assertEquals(SESSION_TOKEN, result.getSessionToken());
  }
}
