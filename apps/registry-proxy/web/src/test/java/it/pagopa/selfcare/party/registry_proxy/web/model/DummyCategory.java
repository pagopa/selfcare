package it.pagopa.selfcare.party.registry_proxy.web.model;

import it.pagopa.selfcare.party.registry_proxy.connector.model.Category;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Origin;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(of = "code")
public class DummyCategory implements Category {

    private String code;
    private String name;
    private String kind;
    private Origin origin;

}