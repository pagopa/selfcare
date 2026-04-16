package it.pagopa.selfcare.party.registry_proxy.connector.model;

import lombok.Data;

@Data
public class QueryFilter {

    private SearchField field;
    private String value;

}
