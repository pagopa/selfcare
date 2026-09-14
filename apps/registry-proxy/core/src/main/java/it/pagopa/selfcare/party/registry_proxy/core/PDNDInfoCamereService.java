package it.pagopa.selfcare.party.registry_proxy.core;

import it.pagopa.selfcare.party.registry_proxy.connector.model.national_registries_pdnd.PDNDBusiness;

import java.util.List;

public interface PDNDInfoCamereService {

    List<PDNDBusiness> retrieveInstitutionsPdndByDescription(String description, String productId);

    PDNDBusiness retrieveInstitutionPdndByTaxCode(String taxCode, String productId);

    PDNDBusiness retrieveInstitutionFromRea(String county, String rea, String productId);

}
