package it.pagopa.selfcare.party.registry_proxy.core;

import it.pagopa.selfcare.party.registry_proxy.connector.model.national_registries.Businesses;
import it.pagopa.selfcare.party.registry_proxy.connector.model.national_registries.LegalAddressProfessionalResponse;
import it.pagopa.selfcare.party.registry_proxy.connector.model.national_registries.VerifyLegalResponse;

public interface NationalRegistriesService {

    LegalAddressProfessionalResponse getLegalAddress(String taxId);

    VerifyLegalResponse verifyLegal(String taxId, String vatNumber);

    Businesses institutionsByLegalTaxId(String legalTaxId);
}
