package it.pagopa.selfcare.party.registry_proxy.connector.api;


import java.util.List;
import java.util.Map;

public interface IndexWriterService<T> {

    void adds(List<? extends T> items);

    /** Writes all items, bypassing any "already up to date" check the implementation may apply. */
    default void addsForced(List<? extends T> items) {
        adds(items);
    }

    void deleteAll();

    void cleanIndex(String entityType);

    void updateDocumentValues(T item, Map<String, String> fieldsToUpdate);

}
