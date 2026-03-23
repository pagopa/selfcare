package it.pagopa.selfcare.party.registry_proxy.connector.model;

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