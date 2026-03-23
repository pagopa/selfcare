package it.pagopa.selfcare.party.registry_proxy.connector.lucene.writer;

import it.pagopa.selfcare.party.registry_proxy.connector.lucene.analysis.AOOTokenAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;

public class DummyAOOIndexWriterFactory implements IndexWriterFactory {

    private final IndexWriterFactory indexWriterFactory;


    public DummyAOOIndexWriterFactory(Directory aoosDirectory) {
        indexWriterFactory = new AOOIndexWriterFactory(aoosDirectory, new AOOTokenAnalyzer());
    }


    @Override
    public IndexWriter create() {
        return indexWriterFactory.create();
    }

}
