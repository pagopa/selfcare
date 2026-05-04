package it.pagopa.selfcare.mscore.connector.rest.model.registryproxy;

import lombok.Data;

@Data
public class PdndResponse {
    private String id;
    private String originId;
    private boolean anacEngaged;
    private boolean anacEnabled;
    private String taxCode;
    private String description;
    private String digitalAddress;
}
