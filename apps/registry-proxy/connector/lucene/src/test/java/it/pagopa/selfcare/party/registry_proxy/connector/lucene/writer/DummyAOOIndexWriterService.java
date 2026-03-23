package it.pagopa.selfcare.party.registry_proxy.connector.lucene.writer;

import it.pagopa.selfcare.party.registry_proxy.connector.api.IndexWriterService;
import it.pagopa.selfcare.party.registry_proxy.connector.model.AOO;

import java.util.List;
import java.util.Map;

public class DummyAOOIndexWriterService implements IndexWriterService<AOO> {

    private final IndexWriterService<AOO> indexWriterService;


    public DummyAOOIndexWriterService(AOOIndexWriterFactory indexWriterFactory) {
        indexWriterService = new AOOIndexWriterService(indexWriterFactory);
    }


    @Override
    public void adds(List<? extends AOO> items) {
        indexWriterService.adds(items);
    }


    @Override
    public void deleteAll() {
        indexWriterService.deleteAll();
    }

    @Override
    public void cleanIndex(String entityType) {
        indexWriterService.cleanIndex(entityType);
    }

    @Override
    public void updateDocumentValues(AOO item, Map<String, String> fieldsToUpdate) {
        indexWriterService.updateDocumentValues(item, fieldsToUpdate);
    }

}
