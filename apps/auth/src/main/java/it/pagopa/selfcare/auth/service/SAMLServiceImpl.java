package it.pagopa.selfcare.auth.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.auth.exception.SamlSignatureException;
import it.pagopa.selfcare.auth.model.UserClaims;
import it.pagopa.selfcare.auth.util.SamlValidator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

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
  SamlValidator samlValidator;

  @Inject
  private final SessionService sessionService;

  @Override
  public Uni<String> generateSessionToken(String samlResponse) throws Exception {
    return samlValidator.validateSamlResponseAsync(samlResponse, idpCert, timeInterval)
        .onItem().transformToUni(this::createSessionToken);
  }

  private Uni<String> createSessionToken(Map<String, String> attributes) {
    return Uni.createFrom().item(attributes)
      .onItem().transformToUni(this::createUserClaims)
      .onItem().transformToUni(sessionService::generateSessionTokenInternal)
      .onFailure().transform(failure -> new SamlSignatureException("SAML validation failed"));
  }

  private Uni<UserClaims> createUserClaims(Map<String, String> attributes) {
    UserClaims userClaims = new UserClaims();
    userClaims.setUid(attributes.get(INTERNAL_ID));
    return Uni.createFrom().item(userClaims);
  }

}
