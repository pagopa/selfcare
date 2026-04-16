package it.pagopa.selfcare.mscore.connector.rest.model.registryproxy;

import it.pagopa.selfcare.mscore.constant.Origin;
import lombok.Data;

@Data
public class AsResponse {
    private String id;
    private String originId;
    private String taxCode;
    private String description;
    private String digitalAddress;
    private String workType;
    private String registerType;
    private String address;
    private Origin origin;
}
