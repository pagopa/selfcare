package it.pagopa.selfcare.party.registry_proxy.connector.rest.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SSLData {
    private String cert;
    private String key;
    private String pub;
}
