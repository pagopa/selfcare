package it.pagopa.selfcare.party.registry_proxy.core;

import it.pagopa.selfcare.party.registry_proxy.connector.model.AOO;
import it.pagopa.selfcare.party.registry_proxy.connector.model.QueryResult;

import java.util.List;


public interface AOOService {

    QueryResult<AOO> findAll(int page, int limit);
    AOO findByUnicode(String codiceUniAOO, List<String> categoriesList);

}
