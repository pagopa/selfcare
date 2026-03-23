package it.pagopa.selfcare.party.registry_proxy.connector.lucene.writer;

import org.apache.lucene.index.IndexWriter;

public interface IndexWriterFactory {

    IndexWriter create();

}
