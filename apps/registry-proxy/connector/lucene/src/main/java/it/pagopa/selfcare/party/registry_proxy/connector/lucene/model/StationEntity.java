package it.pagopa.selfcare.party.registry_proxy.connector.lucene.model;

import it.pagopa.selfcare.party.registry_proxy.connector.model.Origin;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Station;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(of = "taxCode")
public class StationEntity implements Station {

    private String id;
    private String originId;
    private String anacEnabled;
    private String anacEngaged;
    private String taxCode;
    private String description;
    private String digitalAddress;
    private Origin origin;
}
