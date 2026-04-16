package it.pagopa.selfcare.party.registry_proxy.web.model;

import it.pagopa.selfcare.party.registry_proxy.connector.model.Origin;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Station;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(of = "id")
public class DummyPDND implements Station {

    private String id;
    private String originId;
    private String taxCode;
    private String description;
    private String digitalAddress;
    private String anacEnabled;
    private String anacEngaged;
    private Origin origin;

}