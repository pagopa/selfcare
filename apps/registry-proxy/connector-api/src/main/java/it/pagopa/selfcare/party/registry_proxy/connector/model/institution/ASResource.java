package it.pagopa.selfcare.party.registry_proxy.connector.model.institution;

import it.pagopa.selfcare.party.registry_proxy.connector.model.Origin;
import lombok.Data;

@Data
public class ASResource {
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
