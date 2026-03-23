package it.pagopa.selfcare.party.registry_proxy.connector.api;

import it.pagopa.selfcare.party.registry_proxy.connector.model.national_registries_pdnd.PDNDBusiness;
import java.util.List;

public interface PDNDInfoCamereConnector {

    List<PDNDBusiness> retrieveInstitutionsPdndByDescription(String description);

    PDNDBusiness retrieveInstitutionPdndByTaxCode(String taxCode);

    PDNDBusiness retrieveInstitutionDetail(String taxCode);

    byte[] retrieveInstitutionDocument(String taxCode);

    PDNDBusiness retrieveInstitutionFromRea(String county, String rea);

}
