package it.pagopa.selfcare.party.registry_proxy.connector.rest.model.search;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SearchServiceIndexRequest<T extends SearchServiceIndex> {

    private List<T> value;

}
