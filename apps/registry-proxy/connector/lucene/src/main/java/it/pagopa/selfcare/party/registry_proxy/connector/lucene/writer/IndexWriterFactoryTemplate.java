package it.pagopa.selfcare.party.registry_proxy.connector.lucene.writer;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;

@Slf4j
abstract class IndexWriterFactoryTemplate implements IndexWriterFactory {

    private final Directory directory;
    private final Analyzer analyzer;


    public IndexWriterFactoryTemplate(Directory directory, Analyzer analyzer) {
        log.trace("Initializing {}", getClass().getSimpleName());
        this.directory = directory;
        this.analyzer = analyzer;
    }


    @SneakyThrows
    @Override
    public IndexWriter create() {
        log.trace("create start");
        final IndexWriter indexWriter = new IndexWriter(directory, new IndexWriterConfig(analyzer));
        //log.debug("create result = {}", indexWriter);
        log.trace("create end");
        return indexWriter;
    }

}
