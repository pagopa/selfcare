package it.pagopa.selfcare.party.registry_proxy.connector.lucene.writer;

import it.pagopa.selfcare.party.registry_proxy.connector.api.IndexWriterService;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Category;

import java.util.List;
import java.util.Map;

public class DummyCategoryIndexWriterService implements IndexWriterService<Category> {

    private final IndexWriterService<Category> indexWriterService;


    public DummyCategoryIndexWriterService(IndexWriterFactory categoryIndexWriterFactory) {
        indexWriterService = new CategoryIndexWriterService(categoryIndexWriterFactory);
    }


    @Override
    public void adds(List<? extends Category> items) {
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
    public void updateDocumentValues(Category item, Map<String, String> fieldsToUpdate) {
        indexWriterService.updateDocumentValues(item, fieldsToUpdate);
    }

}
