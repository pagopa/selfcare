package it.pagopa.selfcare.onboarding.entity.registry;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.registry.client.ClientRegistryInfocamere;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.product.entity.Product;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.openapi.quarkus.party_registry_proxy_json.api.InfocamereApi;
import org.openapi.quarkus.party_registry_proxy_json.model.BusinessResource;
import org.openapi.quarkus.party_registry_proxy_json.model.BusinessesResource;

public class RegistryManagerInfocamere extends ClientRegistryInfocamere {

  public RegistryManagerInfocamere(
      Onboarding onboarding, InfocamereApi infocamereApi, String managerTaxCode) {
    super(onboarding, infocamereApi, managerTaxCode);
  }

  @Override
  public Uni<Onboarding> customValidation(Product product) {
    return Uni.createFrom().item(onboarding);
  }

  @Override
  public Uni<Boolean> isValid() {
    List<BusinessResource> institutions = super.registryResource.getBusinesses();
    if (institutions.isEmpty() || Objects.isNull(findByTaxCode(institutions))) {
      return Uni.createFrom()
          .failure(
              new InvalidRequestException(
                  String.format(
                      PNPG_INSTITUTION_REGISTRY_NOT_FOUND,
                      onboarding.getInstitution().getTaxCode())));
    }
    return Uni.createFrom().item(true);
  }

  @Override
  public RegistryManager<BusinessesResource> setResource(BusinessesResource registryResource) {
    this.registryResource = registryResource;
    if (Objects.isNull(onboarding.getInstitution().getGeographicTaxonomies())) {
      onboarding.getInstitution().setGeographicTaxonomies(new ArrayList<>());
    }
    return this;
  }
  private BusinessResource findByTaxCode(List<BusinessResource> businessResources) {
    return businessResources.stream()
        .filter(
            businessResource ->
                businessResource
                    .getBusinessTaxId()
                    .equals(onboarding.getInstitution().getTaxCode()))
        .findFirst()
        .orElse(null);
  }
}
