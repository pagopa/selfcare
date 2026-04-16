package it.pagopa.selfcare.party.registry_proxy.connector.rest;

import it.pagopa.selfcare.party.registry_proxy.connector.api.InstitutionConnector;
import it.pagopa.selfcare.party.registry_proxy.connector.model.institution.Institution;
import it.pagopa.selfcare.party.registry_proxy.connector.model.institution.Onboarding;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.client.InstitutionRestClient;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.institution.InstitutionResponse;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.institution.OnboardedProductResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static it.pagopa.selfcare.party.registry_proxy.connector.rest.utils.Const.INSTITUTION_CACHE;

@Slf4j
@Service
public class InstitutionConnectorImpl implements InstitutionConnector {
  private final InstitutionRestClient restClient;

  public InstitutionConnectorImpl(InstitutionRestClient restClient) {
    this.restClient = restClient;
  }

  @Cacheable(value = INSTITUTION_CACHE, key = "#id", cacheManager = INSTITUTION_CACHE)
  public Institution getById(String id) {
    log.debug("InstitutionId = {}", id);
    InstitutionResponse result = restClient.getById(id);
    Institution institution = institutionResponseToInstitution(result);
    log.debug("Institution = {}", institution);
    return institution;
  }

  private Institution institutionResponseToInstitution(InstitutionResponse institutionResponse) {
    Institution institution = new Institution();
    institution.setId(institutionResponse.getId());
    institution.setExternalId(institutionResponse.getExternalId());
    institution.setOrigin(institutionResponse.getOrigin());
    institution.setOriginId(institutionResponse.getOriginId());
    institution.setDescription(institutionResponse.getDescription());
    institution.setInstitutionType(institutionResponse.getInstitutionType());
    institution.setDigitalAddress(institutionResponse.getDigitalAddress());
    institution.setAddress(institutionResponse.getAddress());
    institution.setZipCode(institutionResponse.getZipCode());
    institution.setTaxCode(institutionResponse.getTaxCode());
    institution.setCity(institutionResponse.getCity());
    institution.setCounty(institutionResponse.getCounty());
    institution.setCountry(institutionResponse.getCountry());
    institution.setIstatCode(institutionResponse.getIstatCode());
    institution.setRea(institutionResponse.getRea());
    institution.setShareCapital(institutionResponse.getShareCapital());
    institution.setBusinessRegisterPlace(institutionResponse.getBusinessRegisterPlace());
    institution.setSupportEmail(institutionResponse.getSupportEmail());
    institution.setSupportPhone(institutionResponse.getSupportPhone());
    institution.setImported(institutionResponse.isImported());
    institution.setSubunitCode(institutionResponse.getSubunitCode());
    institution.setSubunitType(institutionResponse.getSubunitType());
    if (Objects.nonNull(institutionResponse.getRootParent())) {
      institution.setRootParentId(institutionResponse.getRootParent().getId());
    }
    institution.setCreatedAt(institutionResponse.getCreatedAt());
    institution.setUpdatedAt(institutionResponse.getUpdatedAt());
    institution.setDelegation(institutionResponse.isDelegation());
    institution.setIsTest(institutionResponse.getIsTest());
    institution.setLegalForm(institutionResponse.getLegalForm());

    institution.setOnboarding(toOnboardingFromResponse(institutionResponse.getOnboarding()));

    return institution;
  }

  private List<Onboarding> toOnboardingFromResponse(List<OnboardedProductResponse> onboardingsResponse) {
    List<Onboarding> onboardings = new ArrayList<>();
    Optional.ofNullable(onboardingsResponse).ifPresent(onboardedProductResponses -> {
      for (OnboardedProductResponse onboardedProductResponse : onboardedProductResponses) {
        Onboarding onboarding = new Onboarding();
        onboarding.setProductId(onboardedProductResponse.getProductId());
        onboarding.setTokenId(onboardedProductResponse.getTokenId());
        onboarding.setStatus(onboardedProductResponse.getStatus().name());
        onboarding.setOrigin(onboardedProductResponse.getOrigin());
        onboarding.setOriginId(onboardedProductResponse.getOriginId());
        onboarding.setInstitutionType(onboardedProductResponse.getInstitutionType());
        onboarding.setIsAggregator(onboardedProductResponse.getIsAggregator());
        onboardings.add(onboarding);
      }
    });

    return onboardings;
  }
}