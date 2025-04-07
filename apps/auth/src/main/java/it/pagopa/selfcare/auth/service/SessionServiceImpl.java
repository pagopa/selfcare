package it.pagopa.selfcare.auth.service;

import io.smallrye.jwt.build.Jwt;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.auth.exception.InternalException;
import it.pagopa.selfcare.auth.util.GeneralUtils;
import it.pagopa.selfcare.auth.util.Pkcs8Utils;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Duration;
import java.time.Instant;

import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.user_registry_json.api.UserApi;
import org.openapi.quarkus.user_registry_json.model.FamilyNameCertifiableSchema;
import org.openapi.quarkus.user_registry_json.model.NameCertifiableSchema;
import org.openapi.quarkus.user_registry_json.model.SaveUserDto;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

  private static final String SPID_FISCAL_NUMBER_PREFIX = "TINIT-";
  private static final String SPID_LEVEL_L2 = "https://www.spid.gov.it/SpidL2";
  private static final String ISSUER = "SPID";

  @ConfigProperty(name = "jwt.session.duration")
  Integer sessionDuration;

  @ConfigProperty(name = "jwt.session.audience")
  String audience;

  @ConfigProperty(name = "jwt.session.private.key")
  String privateKeyPem;

  @ConfigProperty(name = "auth-ms.retry.min-backoff")
  Integer retryMinBackOff;

  @ConfigProperty(name = "auth-ms.retry.max-backoff")
  Integer retryMaxBackOff;

  @ConfigProperty(name = "auth-ms.retry")
  Integer maxRetry;

  @RestClient @Inject UserApi userRegistryApi;

  @Override
  public Uni<String> generateSessionToken(String fiscalNumber, String name, String familyName) {
    String fiscalCode = fiscalNumber.replace(SPID_FISCAL_NUMBER_PREFIX, "");
    SaveUserDto saveUserDto = new SaveUserDto();
    saveUserDto.name(
        NameCertifiableSchema.builder()
            .certification(NameCertifiableSchema.CertificationEnum.SPID)
            .value(name)
            .build());
    saveUserDto.familyName(
        FamilyNameCertifiableSchema.builder()
            .certification(FamilyNameCertifiableSchema.CertificationEnum.SPID)
            .value(familyName)
            .build());
    saveUserDto.fiscalCode(fiscalCode);
    return userRegistryApi
        .saveUsingPATCH(saveUserDto)
        .onFailure(GeneralUtils::checkIfIsRetryableException)
        .retry()
        .withBackOff(Duration.ofSeconds(retryMinBackOff), Duration.ofSeconds(retryMaxBackOff))
        .atMost(maxRetry)
        .onFailure(WebApplicationException.class)
        .transform(GeneralUtils::extractExceptionFromWebAppException)
        .chain(
            userId ->
                Pkcs8Utils.extractRSAPrivateKeyFromPem(privateKeyPem)
                    .onFailure()
                    .transform(ex -> new InternalException(ex.getMessage()))
                    .map(
                        rsaPrivateKey ->
                            Jwt.claims()
                                .claim("fiscal_number", fiscalCode)
                                .claim("name", name)
                                .claim("family_name", familyName)
                                .claim("uid", userId.getId().toString())
                                .claim("spid_level", SPID_LEVEL_L2)
                                .issuer(ISSUER)
                                .audience(audience)
                                .issuedAt(Instant.now())
                                .expiresAt(Instant.now().plus(Duration.ofHours(sessionDuration)))
                                .sign(rsaPrivateKey)));
  }
}
