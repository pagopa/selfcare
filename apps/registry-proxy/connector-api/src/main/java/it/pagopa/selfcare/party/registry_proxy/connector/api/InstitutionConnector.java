package it.pagopa.selfcare.party.registry_proxy.connector.api;

import it.pagopa.selfcare.party.registry_proxy.connector.model.institution.Institution;

public interface InstitutionConnector {

    Institution getById(String id);
}
