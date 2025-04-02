package it.pagopa.selfcare.auth.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.auth.controller.response.OidcExchangeResponse;
import it.pagopa.selfcare.auth.exception.InternalException;
import it.pagopa.selfcare.auth.util.GeneralUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.one_identity_json.api.DefaultApi;
import org.openapi.quarkus.one_identity_json.api.DefaultApi.CreateRequestTokenMultipartForm;
import org.openapi.quarkus.one_identity_json.model.TokenData;

import java.time.Duration;
import java.util.Base64;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class OidcServiceImpl implements OidcService {

  public static final String AUTH_CODE_GRANT_TYPE = "authorization_code";
  private final JwtService jwtService;

  private final SessionService sessionService;

  @ConfigProperty(name = "auth-ms.retry.min-backoff")
  Integer retryMinBackOff;

  @ConfigProperty(name = "auth-ms.retry.max-backoff")
  Integer retryMaxBackOff;

  @ConfigProperty(name = "auth-ms.retry")
  Integer maxRetry;

  @ConfigProperty(name = "one-identity.client-id")
  String oiClientId;

  @ConfigProperty(name = "one-identity.client-secret")
  String oiClientSecret;

  @RestClient @Inject DefaultApi tokenApi;

  @Override
  public Uni<OidcExchangeResponse> exchange(String authCode, String redirectUri) {
    CreateRequestTokenMultipartForm formData = new CreateRequestTokenMultipartForm();
    formData.code = authCode;
    formData.grantType = AUTH_CODE_GRANT_TYPE;
    formData.redirectUri = redirectUri;
    return tokenApi
        .createRequestToken(
            formData,
            Base64.getEncoder()
                .encodeToString(String.join(":", oiClientId, oiClientSecret).getBytes()))
        .onFailure(GeneralUtils::checkIfIsRetryableException)
        .retry()
        .withBackOff(Duration.ofSeconds(retryMinBackOff), Duration.ofSeconds(retryMaxBackOff))
        .atMost(maxRetry)
        .onFailure(WebApplicationException.class)
        .transform(GeneralUtils::extractExceptionFromWebAppException)
        .map(TokenData::getIdToken)
        .chain(
            idToken ->
                jwtService
                    .extractClaimsFromJwtToken(idToken)
                    .onFailure()
                    .transform(
                        failure ->
                            new InternalException("Cannot parse idToken from authorization code")))
        .chain(
            claims ->
                sessionService.generateSessionToken(
                    claims.get("fiscal_number"), claims.get("name"), claims.get("family_name")))
        .map(sessionToken -> OidcExchangeResponse.builder().sessionToken(sessionToken).build());
  }
}
