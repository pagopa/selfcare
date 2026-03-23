package it.pagopa.selfcare.party.registry_proxy.connector.lucene.model;

import lombok.Getter;

@Getter
public class IndexChangedEvent {

    private Class<? extends DocumentType> entityType;

    public IndexChangedEvent(Class<? extends DocumentType> entityType) {
        this.entityType = entityType;
    }


}
