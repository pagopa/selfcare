package it.pagopa.selfcare.auth.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.auth.controller.response.OidcExchangeOtpResponse;
import it.pagopa.selfcare.auth.controller.response.OidcExchangeResponse;
import it.pagopa.selfcare.auth.controller.response.OidcExchangeTokenResponse;
import it.pagopa.selfcare.auth.exception.InternalException;
import it.pagopa.selfcare.auth.model.otp.OtpInfo;
import it.pagopa.selfcare.auth.util.GeneralUtils;
import it.pagopa.selfcare.auth.util.OtpUtils;
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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class OidcServiceImpl implements OidcService {

  public static final String AUTH_CODE_GRANT_TYPE = "authorization_code";
  private final JwtService jwtService;

  private final SessionService sessionService;

  private final UserService userService;

  private final OtpFlowService otpFlowService;

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
                .encodeToString(
                    String.join(
                            ":",
                            URLEncoder.encode(oiClientId, StandardCharsets.UTF_8),
                            URLEncoder.encode(oiClientSecret, StandardCharsets.UTF_8))
                        .getBytes()))
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
                userService
                    .patchUser(
                        claims.get("fiscalNumber"),
                        claims.get("name"),
                        claims.get("familyName"),
                        Boolean.valueOf(claims.get("sameIdp")))
                    .onFailure()
                    .transform(
                        failure ->
                            new InternalException(
                                "Cannot patch user on Personal Data Vault:" + failure.toString())))
        .chain(
            userClaims ->
                otpFlowService
                    .handleOtpFlow(userClaims)
                    .onFailure()
                    .transform(
                        failure ->
                            new InternalException("Cannot Handle OTP Flow:" + failure.toString()))
                    .chain(
                        maybeOtpInfo ->
                                maybeOtpInfo
                                .map(
                                    otpInfo ->
                                        Uni.createFrom().item(newOidcExchangeOtpResponse(otpInfo)))
                                .orElse(
                                    sessionService
                                        .generateSessionToken(userClaims)
                                        .onFailure()
                                        .transform(
                                            failure ->
                                                new InternalException(
                                                    "Cannot generate session token:"
                                                        + failure.toString()))
                                        .map(this::newOidcExchangeTokenResponse))));
  }

  private OidcExchangeResponse newOidcExchangeOtpResponse(OtpInfo otpInfo) {
    return new OidcExchangeOtpResponse(
        otpInfo.getUuid(), OtpUtils.maskEmail(otpInfo.getInstitutionalEmail()));
  }

  private OidcExchangeResponse newOidcExchangeTokenResponse(String sessionToken) {
    return new OidcExchangeTokenResponse(sessionToken);
  }


}
