package it.pagopa.selfcare.party.registry_proxy.connector.lucene.writer;

import it.pagopa.selfcare.party.registry_proxy.connector.lucene.analysis.UOTokenAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;

public class DummyUOIndexWriterFactory implements IndexWriterFactory {

    private final IndexWriterFactory indexWriterFactory;


    public DummyUOIndexWriterFactory(Directory uosDirectory) {
        indexWriterFactory = new UOIndexWriterFactory(uosDirectory, new UOTokenAnalyzer());
    }


    @Override
    public IndexWriter create() {
        return indexWriterFactory.create();
    }

}
