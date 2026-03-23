package it.pagopa.selfcare.party.registry_proxy.connector.lucene.writer;

import it.pagopa.selfcare.party.registry_proxy.connector.lucene.analysis.InstitutionTokenAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;

public class DummyInstitutionIndexWriterFactory implements IndexWriterFactory {

    private final IndexWriterFactory indexWriterFactory;


    public DummyInstitutionIndexWriterFactory(Directory institutionsDirectory) {
        indexWriterFactory = new InstitutionIndexWriterFactory(institutionsDirectory, new InstitutionTokenAnalyzer());
    }


    @Override
    public IndexWriter create() {
        return indexWriterFactory.create();
    }

}
