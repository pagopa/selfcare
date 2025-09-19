package it.pagopa.selfcare.auth.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.auth.util.SamlValidator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

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

  @Override
  public Uni<Boolean> validate(String samlResponse) throws Exception {
    return samlValidator.validateSamlResponseAsync(samlResponse, idpCert, timeInterval);
  }

}
