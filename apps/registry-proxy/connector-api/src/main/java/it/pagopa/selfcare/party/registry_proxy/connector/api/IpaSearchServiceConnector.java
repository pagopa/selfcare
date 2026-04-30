package it.pagopa.selfcare.party.registry_proxy.connector.api;

import it.pagopa.selfcare.party.registry_proxy.connector.model.Institution;
import it.pagopa.selfcare.party.registry_proxy.connector.model.IpaInstitutionSearchResult;

public interface IpaSearchServiceConnector
    extends SearchConnector<Institution, IpaInstitutionSearchResult> {}
