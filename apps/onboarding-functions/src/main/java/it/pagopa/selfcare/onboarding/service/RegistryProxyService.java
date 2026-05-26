package it.pagopa.selfcare.onboarding.service;

public interface RegistryProxyService {
    byte[] getInstitutionVisuraByTaxCode(String taxCode);
}
