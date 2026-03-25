package it.pagopa.selfcare.party.registry_proxy.connector.lucene.writer;

import it.pagopa.selfcare.party.registry_proxy.connector.api.IndexWriterService;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Institution;

import java.util.List;
import java.util.Map;

public class DummyInstitutionIndexWriterService implements IndexWriterService<Institution> {

    private final IndexWriterService<Institution> indexWriterService;


    public DummyInstitutionIndexWriterService(IndexWriterFactory institutionIndexWriterFactory) {
        indexWriterService = new InstitutionIndexWriterService(institutionIndexWriterFactory);
    }


    @Override
    public void adds(List<? extends Institution> items) {
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
    public void updateDocumentValues(Institution item, Map<String, String> fieldsToUpdate) {
        indexWriterService.updateDocumentValues(item, fieldsToUpdate);
    }

}
