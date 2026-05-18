package it.pagopa.selfcare.onboarding.service.impl;

import it.pagopa.selfcare.onboarding.service.InfocamereService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.party_registry_proxy_json.api.PdndVisuraInfoCamereControllerApi;

@ApplicationScoped
@Slf4j
public class InfocamereServiceImpl implements InfocamereService {

  private final PdndVisuraInfoCamereControllerApi pdndVisuraInfoCamereApi;

  @Inject
  public InfocamereServiceImpl(@RestClient PdndVisuraInfoCamereControllerApi pdndVisuraInfoCamereApi) {
    this.pdndVisuraInfoCamereApi = pdndVisuraInfoCamereApi;
  }

  @Override
  public byte[] getInstitutionVisuraByTaxCode(String taxCode) {
    log.debug("Retrieving institution visura: taxCode={}", taxCode);
    return pdndVisuraInfoCamereApi.institutionVisuraDocumentByTaxCodeUsingGET(taxCode);
  }
}
