package it.pagopa.selfcare.party.registry_proxy.connector.api;


import java.util.List;
import java.util.Map;

public interface IndexWriterService<T> {

    void adds(List<? extends T> items);

    void deleteAll();

    void cleanIndex(String entityType);

    void updateDocumentValues(T item, Map<String, String> fieldsToUpdate);

}
