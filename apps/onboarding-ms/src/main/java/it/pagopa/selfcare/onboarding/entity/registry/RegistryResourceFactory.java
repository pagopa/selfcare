package it.pagopa.selfcare.onboarding.entity.registry;

import static it.pagopa.selfcare.onboarding.common.InstitutionPaSubunitType.EC;
import static it.pagopa.selfcare.onboarding.common.InstitutionType.GSP;
import static it.pagopa.selfcare.onboarding.common.InstitutionType.PT;

import it.pagopa.selfcare.onboarding.common.Origin;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.party_registry_proxy_json.api.*;
import org.openapi.quarkus.user_registry_json.api.UserApi;

import java.util.Optional;

@ApplicationScoped
public class RegistryResourceFactory {

    @RestClient
    @Inject
    org.openapi.quarkus.party_registry_proxy_json.api.InstitutionApi institutionApi;

    @RestClient
    @Inject
    InfocamereApi infocamereApi;

    @RestClient
    @Inject
    NationalRegistriesApi nationalRegistriesApi;

    @RestClient
    @Inject
    AooApi aooApi;

    @RestClient
    @Inject
    UoApi uoApi;

    @RestClient
    @Inject
    InfocamerePdndApi infocamerePdndApi;

    @RestClient
    @Inject
    InsuranceCompaniesApi insuranceCompaniesApi;

    @RestClient
    @Inject
    StationsApi stationsApi;

    @RestClient
    @Inject
    UserApi userRegistryApi;

    @RestClient
    @Inject
    GeographicTaxonomiesApi geographicTaxonomiesApi;

    @RestClient
    @Inject
    PdndVisuraInfoCamereControllerApi pdndVisuraInfoCamereControllerApi;

    @ConfigProperty(name = "onboarding-ms.allowed-ateco-codes")
    Optional<String> allowedAtecoCodes;

  public RegistryManager<?> create(Onboarding onboarding, String managerTaxCode) {
    return switch (onboarding.getInstitution().getOrigin() != null
        ? onboarding.getInstitution().getOrigin()
        : Origin.SELC) {
      case PDND_INFOCAMERE -> new RegistryManagerPDNDInfocamere(onboarding, infocamerePdndApi, userRegistryApi, allowedAtecoCodes, pdndVisuraInfoCamereControllerApi);
      case ANAC -> new RegistryManagerANAC(onboarding, stationsApi);
      case IVASS -> new RegistryManagerIVASS(onboarding, insuranceCompaniesApi);
      case INFOCAMERE -> new RegistryManagerInfocamere(onboarding, infocamereApi, managerTaxCode);
      case ADE -> new RegistryManagerADE(onboarding, nationalRegistriesApi, managerTaxCode);
      case IPA -> getResourceFromIPA(onboarding);
      default -> getRegistryManagerSELC(onboarding);
    };
  }

  private RegistryManager<?> getResourceFromIPA(Onboarding onboarding) {
    return switch (onboarding.getInstitution().getSubunitType() != null
        ? onboarding.getInstitution().getSubunitType()
        : EC) {
      case AOO -> new RegistryManagerIPAAoo(onboarding, uoApi, aooApi);
      case UO -> new RegistryManagerIPAUo(onboarding, uoApi);
      default -> getResourceFromInstitutionType(onboarding);
    };
  }

  private RegistryManager<?> getRegistryManagerSELC(Onboarding onboarding) {
    if (PT.equals(onboarding.getInstitution().getInstitutionType())) {
      return new RegistryManagerPT(onboarding);
    }
    return new RegistryManagerSELC(onboarding);
  }

  private RegistryManager<?> getResourceFromInstitutionType(Onboarding onboarding) {
    if (GSP.equals(onboarding.getInstitution().getInstitutionType())) {
      return new RegistryManagerIPAGps(onboarding, uoApi, institutionApi);
    }
    return new RegistryManagerIPA(onboarding, uoApi, institutionApi, geographicTaxonomiesApi);
  }
}
