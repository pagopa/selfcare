package it.pagopa.selfcare.party.registry_proxy.connector.api;

import it.pagopa.selfcare.party.registry_proxy.connector.model.ResourceResponse;
import java.io.InputStream;

public interface FileStorageConnector {
    ResourceResponse getFile(String fileName);

    void uploadFile(InputStream file, String fileName);

}
