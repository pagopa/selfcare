package it.pagopa.selfcare.mscore.connector.rest.model.registryproxy;

import lombok.Data;

@Data
public class ProxyCategoryResponse {
    private String code;
    private String name;
    private String kind;
    private String origin;
}
