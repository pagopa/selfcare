package it.pagopa.selfcare.party.registry_proxy.connector.lucene.writer;

import it.pagopa.selfcare.party.registry_proxy.connector.lucene.analysis.CategoryTokenAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;

public class DummyCategoryIndexWriterFactory implements IndexWriterFactory {

    private final IndexWriterFactory indexWriterFactory;


    public DummyCategoryIndexWriterFactory(Directory categoriesDirectory) {
        indexWriterFactory = new CategoryIndexWriterFactory(categoriesDirectory, new CategoryTokenAnalyzer());
    }


    @Override
    public IndexWriter create() {
        return indexWriterFactory.create();
    }

}
