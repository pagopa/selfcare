package it.pagopa.selfcare.mscore.connector.rest.model.registryproxy;

import lombok.Data;

@Data
public class ProxyInstitutionResponse {
    private String id;
    private String originId;
    private String o;
    private String ou;
    private String aoo;
    private String taxCode;
    private String category;
    private String description;
    private String digitalAddress;
    private String address;
    private String zipCode;
    private String origin;
    private String istatCode;
}
