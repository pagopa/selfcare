package it.pagopa.selfcare.party.registry_proxy.connector.rest.client;

public interface OpenDataRestClient {

    String retrieveInstitutions();

    String retrieveCategories();

    String retrieveAOOs();

    String retrieveUOs();

    String retrieveUOsWithSfe();

}
