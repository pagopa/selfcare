package it.pagopa.selfcare.external_api.connector;


import it.pagopa.selfcare.external_api.model.document.ResourceResponse;

public interface FileStorageConnector {

    ResourceResponse getFile(String fileName);

}
