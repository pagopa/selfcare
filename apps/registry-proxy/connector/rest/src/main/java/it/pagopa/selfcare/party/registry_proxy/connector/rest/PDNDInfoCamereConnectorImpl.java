package it.pagopa.selfcare.party.registry_proxy.connector.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.onboarding.crypto.utils.DataEncryptionUtils;
import it.pagopa.selfcare.party.registry_proxy.connector.api.PDNDInfoCamereConnector;
import it.pagopa.selfcare.party.registry_proxy.connector.exception.ResourceNotFoundException;
import it.pagopa.selfcare.party.registry_proxy.connector.model.national_registries_pdnd.PDNDBusiness;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.client.PDNDInfoCamereRestClient;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.config.PdndSecretValueResolver;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.ClientCredentialsResponse;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.PDNDImpresa;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.PdndSecretValue;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.mapper.PDNDBusinessMapper;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class PDNDInfoCamereConnectorImpl implements PDNDInfoCamereConnector {
  private static final String TAX_CODE_REQUIRED_MESSAGE = "TaxCode is required";
  private final PDNDInfoCamereRestClient pdndInfoCamereRestClient;
  private final PDNDBusinessMapper pdndBusinessMapper;
  private final TokenProvider tokenProvider;
  private final PdndSecretValueResolver pdndSecretValueResolver;
  private final PDNDCacheableService PDNDCacheableService;
  private static final String BEARER = "Bearer ";

  public PDNDInfoCamereConnectorImpl(
          PDNDInfoCamereRestClient pdndInfoCamereRestClient,
          PDNDBusinessMapper pdndBusinessMapper,
          TokenProviderPDND tokenProviderPDND,
          PdndSecretValueResolver pdndSecretValueResolver,
          PDNDCacheableService PDNDCacheableService) {
    this.pdndInfoCamereRestClient = pdndInfoCamereRestClient;
    this.pdndBusinessMapper = pdndBusinessMapper;
    this.tokenProvider = tokenProviderPDND;
    this.pdndSecretValueResolver = pdndSecretValueResolver;
    this.PDNDCacheableService = PDNDCacheableService;
  }

  @Override
  public List<PDNDBusiness> retrieveInstitutionsPdndByDescription(String description, String productId) {
    Assert.hasText(description, "Description is required");
    PdndSecretValue secretValue = pdndSecretValueResolver.resolve(productId);
    ClientCredentialsResponse tokenResponse = tokenProvider.getTokenPdnd(secretValue);
    String bearer = BEARER + tokenResponse.getAccessToken();
    List<PDNDImpresa> result = pdndInfoCamereRestClient.retrieveInstitutionsPdndByDescription(description, bearer);
    return pdndBusinessMapper.toPDNDBusinesses(result);
  }

  @Override
  public PDNDBusiness retrieveInstitutionPdndByTaxCode(String taxCode, String productId) {
      Assert.hasText(taxCode, TAX_CODE_REQUIRED_MESSAGE);
      String encTaxCode = DataEncryptionUtils.encrypt(taxCode);
      PdndSecretValue secretValue = pdndSecretValueResolver.resolve(productId);
      PDNDImpresa impresa = null;

      try {
          String encResult = PDNDCacheableService.getEncryptedPDNDImpresa(encTaxCode, secretValue);
          String decResult = DataEncryptionUtils.decrypt(encResult);
          impresa = new ObjectMapper().readValue(decResult, new TypeReference<>(){});
      } catch (Exception e) {
          log.error("Errore", e);
      }

      return pdndBusinessMapper.toPDNDBusiness(impresa);
  }

  @Override
  public PDNDBusiness retrieveInstitutionFromRea(String county, String rea, String productId) {
    Assert.hasText(rea, "Rea is required");
    Assert.hasText(county, "County is required");
    PdndSecretValue secretValue = pdndSecretValueResolver.resolve(productId);
    ClientCredentialsResponse tokenResponse = tokenProvider.getTokenPdnd(secretValue);
    String bearer = BEARER + tokenResponse.getAccessToken();
    List<PDNDImpresa> institutions = pdndInfoCamereRestClient.retrieveInstitutionPdndFromRea(rea, county, bearer);
    if (Objects.isNull(institutions) || institutions.isEmpty()) {
      throw new ResourceNotFoundException("No institution found with rea: " + county + "-" + rea);
    }
    PDNDImpresa result  = institutions.get(0);
    return pdndBusinessMapper.toPDNDBusiness(result);
  }
}
