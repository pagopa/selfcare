package it.pagopa.selfcare.auth.service;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.jwt.auth.principal.ParseException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.auth.exception.ForbiddenException;
import it.pagopa.selfcare.auth.exception.InternalException;
import it.pagopa.selfcare.auth.exception.ResourceNotFoundException;
import it.pagopa.selfcare.auth.model.UserClaims;
import jakarta.inject.Inject;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.openapi.quarkus.one_identity_json.api.DefaultApi;
import org.openapi.quarkus.one_identity_json.model.TokenData;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@QuarkusTest
public class OidcServiceTest {

  @Inject OidcService oidcService;
  @InjectMock SessionService sessionService;
  @InjectMock JwtService jwtService;
  @InjectMock UserService userService;

  @RestClient @InjectMock DefaultApi tokenApi;

  @Test
  void exchangeAuthCodeWithSessionToken() throws ParseException {

    when(tokenApi.createRequestToken(any(), anyString()))
        .thenReturn(Uni.createFrom().item(TokenData.builder().idToken("idToken").build()));
    when(jwtService.extractClaimsFromJwtToken(anyString()))
        .thenReturn(
            Uni.createFrom()
                .item(
                    Map.of(
                        "fiscal_number",
                        "TINIT-FISCALCODE",
                        "name",
                        "name",
                        "family_name",
                        "Doe")));

    when(userService.patchUser(anyString(), anyString(), anyString(), anyBoolean()))
        .thenReturn(Uni.createFrom().item(UserClaims.builder().fiscalCode("").build()));

    when(sessionService.generateSessionToken(any(UserClaims.class)))
        .thenReturn(Uni.createFrom().item(""));

    oidcService
        .exchange("authCode", "redirectUri")
        .subscribe()
        .withSubscriber(UniAssertSubscriber.create())
        .assertCompleted();
  }

  @Test
  void failureWithCreateRequestTokenError() throws ParseException {
    String exceptionDesc = "Cannot invoke requestToken";

    when(tokenApi.createRequestToken(any(), anyString()))
        .thenReturn(Uni.createFrom().failure(new WebApplicationException(exceptionDesc)));

    oidcService
        .exchange("authCode", "redirectUri")
        .subscribe()
        .withSubscriber(UniAssertSubscriber.create())
        .assertFailed()
        .assertFailedWith(InternalException.class, exceptionDesc);
  }

  @Test
  void failureWithCreateRequestTokenForbidden() throws ParseException {

    when(tokenApi.createRequestToken(any(), anyString()))
        .thenReturn(
            Uni.createFrom().failure(new WebApplicationException(Response.status(403).build())));

    oidcService
        .exchange("authCode", "redirectUri")
        .subscribe()
        .withSubscriber(UniAssertSubscriber.create())
        .assertFailed()
        .assertFailedWith(ForbiddenException.class, "Forbidden");
  }

  @Test
  void failureWithCreateRequestTokenNotFound() throws ParseException {

    when(tokenApi.createRequestToken(any(), anyString()))
        .thenReturn(
            Uni.createFrom().failure(new WebApplicationException(Response.status(404).build())));

    oidcService
        .exchange("authCode", "redirectUri")
        .subscribe()
        .withSubscriber(UniAssertSubscriber.create())
        .assertFailed()
        .assertFailedWith(ResourceNotFoundException.class, "Not Found");
  }

  @Test
  void failureWhenExtractingIdTokenClaims() throws ParseException {

    when(tokenApi.createRequestToken(any(), anyString()))
        .thenReturn(Uni.createFrom().item(TokenData.builder().idToken("invalidIdToken").build()));
    when(jwtService.extractClaimsFromJwtToken(anyString()))
        .thenReturn(Uni.createFrom().failure(new Exception("Error")));

    oidcService
        .exchange("authCode", "redirectUri")
        .subscribe()
        .withSubscriber(UniAssertSubscriber.create())
        .assertFailed()
        .assertFailedWith(Exception.class, "Cannot parse idToken from authorization code");
  }

  @Test
  void failureWhenPatchingUser() throws ParseException {
    String exceptionDesc = "Error while patching user";
    when(tokenApi.createRequestToken(any(), anyString()))
            .thenReturn(Uni.createFrom().item(TokenData.builder().idToken("idToken").build()));
    when(jwtService.extractClaimsFromJwtToken(anyString()))
            .thenReturn(
                    Uni.createFrom()
                            .item(
                                    Map.of(
                                            "fiscalNumber", "TINIT-FISCALCODE", "name", "name", "familyName", "Doe")));

    when(userService.patchUser(anyString(), anyString(), anyString(), anyBoolean()))
            .thenReturn(Uni.createFrom().failure(new Exception(exceptionDesc)));

    oidcService
            .exchange("authCode", "redirectUri")
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create())
            .assertFailed()
            .assertFailedWith(Exception.class, exceptionDesc);
  }

  @Test
  void failureWhenGeneratingSessionToken() throws ParseException {
    String exceptionDesc = "Cannot generate sessionToken";
    when(tokenApi.createRequestToken(any(), anyString()))
        .thenReturn(Uni.createFrom().item(TokenData.builder().idToken("idToken").build()));
    when(jwtService.extractClaimsFromJwtToken(anyString()))
        .thenReturn(
            Uni.createFrom()
                .item(
                    Map.of(
                        "fiscalNumber", "TINIT-FISCALCODE", "name", "name", "familyName", "Doe")));

    when(userService.patchUser(anyString(), anyString(), anyString(), anyBoolean()))
            .thenReturn(Uni.createFrom().item(UserClaims.builder().fiscalCode("").build()));
    when(sessionService.generateSessionToken(any(UserClaims.class)))
        .thenReturn(Uni.createFrom().failure(new Exception(exceptionDesc)));

    oidcService
        .exchange("authCode", "redirectUri")
        .subscribe()
        .withSubscriber(UniAssertSubscriber.create())
        .assertFailed()
        .assertFailedWith(Exception.class, exceptionDesc);
  }
}
