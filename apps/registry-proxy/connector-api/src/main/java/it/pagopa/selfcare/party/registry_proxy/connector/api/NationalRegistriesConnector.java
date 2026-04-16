package it.pagopa.selfcare.party.registry_proxy.connector.api;

import it.pagopa.selfcare.party.registry_proxy.connector.model.national_registries.Businesses;
import it.pagopa.selfcare.party.registry_proxy.connector.model.national_registries.LegalAddressResponse;
import it.pagopa.selfcare.party.registry_proxy.connector.model.national_registries.VerifyLegalResponse;

public interface NationalRegistriesConnector{

    LegalAddressResponse getLegalAddress(String taxCode);

    VerifyLegalResponse verifyLegal(String taxId, String vatNumber);

    Businesses institutionsByLegalTaxId(String legalTaxId);
}
