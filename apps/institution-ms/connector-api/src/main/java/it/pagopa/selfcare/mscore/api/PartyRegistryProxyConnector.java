package it.pagopa.selfcare.mscore.api;

import it.pagopa.selfcare.mscore.model.AreaOrganizzativaOmogenea;
import it.pagopa.selfcare.mscore.model.UnitaOrganizzativa;
import it.pagopa.selfcare.mscore.model.institution.*;

import java.util.List;

public interface PartyRegistryProxyConnector {

    InstitutionProxyInfo getInstitutionById(String id);

    CategoryProxyInfo getCategory(String origin, String code);

    List<InstitutionByLegal> getInstitutionsByLegal(String taxId);

    NationalRegistriesProfessionalAddress getLegalAddress(String taxId);

    GeographicTaxonomies getExtByCode(String code);

    AreaOrganizzativaOmogenea getAooById(String aooId);

    UnitaOrganizzativa getUoById(String uoId);

    SaResource getSAFromAnac(String taxId);

    ASResource getASFromIvass(String ivassCode);
}
