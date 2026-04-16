package it.pagopa.selfcare.party.registry_proxy.connector.model;

import lombok.Data;

@Data
public class ResourceResponse {
    private byte[] data;
    private String fileName;
    private String mimetype;
}
