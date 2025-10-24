package it.pagopa.selfcare.auth.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.auth.client.IamMsApi;
import it.pagopa.selfcare.auth.context.TokenContext;
import it.pagopa.selfcare.auth.exception.SamlSignatureException;
import it.pagopa.selfcare.auth.model.UserClaims;
import it.pagopa.selfcare.auth.util.GeneralUtils;
import it.pagopa.selfcare.auth.util.SamlValidator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.iam_json.model.SaveUserRequest;

import java.time.Duration;
import java.util.Map;

import static it.pagopa.selfcare.auth.util.SamlValidator.INTERNAL_ID;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class SAMLServiceImpl implements SAMLService {

  @Inject
  @ConfigProperty(name = "saml.idp.entity-id")
  String idpEntityId;

  @Inject
  @ConfigProperty(name = "saml.idp.cert")
  String idpCert;

  @Inject
  @ConfigProperty(name = "saml.sp.entity-id")
  String spEntityId;

  @Inject
  @ConfigProperty(name = "saml.sp.acs.url")
  String spAcsUrl;

  @Inject
  @ConfigProperty(name = "saml.time.interval")
  long timeInterval;

  @Inject
  @ConfigProperty(name = "auth_fe.url.login.success")
  String loginSuccessUrl;

  @ConfigProperty(name = "auth-ms.retry.min-backoff")
  Integer retryMinBackOff;

  @ConfigProperty(name = "auth-ms.retry.max-backoff")
  Integer retryMaxBackOff;

  @ConfigProperty(name = "auth-ms.retry")
  Integer maxRetry;

  @Inject
  SamlValidator samlValidator;

  @Inject
  private final SessionService sessionService;

  @Inject
  TokenContext tokenContext;

  @RestClient @Inject
  IamMsApi iamApi;

  @Override
  public Uni<String> generateSessionToken(String samlResponse) throws Exception {
    return samlValidator.validateSamlResponseAsync(samlResponse, idpCert, timeInterval)
        .onItem().transformToUni(this::createSessionToken);
  }

  private Uni<String> createSessionToken(Map<String, String> attributes) {
    return Uni.createFrom().item(attributes)
      .onItem().transformToUni(this::createUserClaims)
      .onItem().transformToUni(userClaims ->
        sessionService.generateSessionTokenInternal(userClaims)
          .onItem().transform(token -> tokenContext.setToken(token))
          .onItem().transformToUni(token -> saveUser(userClaims.getEmail()))
      )
      .onItem().transformToUni(sessionService::generateSessionTokenInternal)
      .onFailure().transform(failure -> new SamlSignatureException("SAML validation failed"));
  }

  private Uni<UserClaims> createUserClaims(Map<String, String> attributes) {
    UserClaims userClaims = new UserClaims();
    userClaims.setUid(attributes.get(INTERNAL_ID));
    userClaims.setEmail(attributes.get(INTERNAL_ID));
    return Uni.createFrom().item(userClaims);
  }

  @Override
  public String getLoginSuccessUrl(String token) {
    return spEntityId + loginSuccessUrl + "#token=" + token;
  }

  public Uni<UserClaims> saveUser(String email) {
    return Uni.createFrom().item(email)
      .onItem().transform(mail -> {
      SaveUserRequest saveUserRequest = new SaveUserRequest();
      saveUserRequest.setEmail(mail);
      return saveUserRequest;
      })
      .onItem().transformToUni(saveUserRequest -> iamApi.saveIAMUser(saveUserRequest, null))
      .onFailure(GeneralUtils::checkIfIsRetryableException)
      .retry()
      .withBackOff(Duration.ofSeconds(retryMinBackOff), Duration.ofSeconds(retryMaxBackOff))
      .atMost(maxRetry)
      .onFailure(WebApplicationException.class)
      .transform(GeneralUtils::extractExceptionFromWebAppException)
      .onItem().transform(userClaims ->
        UserClaims.builder()
          .uid(userClaims.getUid())
          .email(userClaims.getEmail())
          .name(userClaims.getName())
          .familyName(userClaims.getFamilyName())
          .test(userClaims.getTest())
          .build()
      );
  }
}
