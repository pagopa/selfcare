package it.pagopa.selfcare.party.registry_proxy.connector.lucene.reader;

import lombok.SneakyThrows;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexNotFoundException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.RAMDirectory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.*;

class DirectoryReaderFactoryTest {

    private final RAMDirectory directory = new RAMDirectory();
    private final DirectoryReaderFactory directoryReaderFactory = new DirectoryReaderFactory(directory);


    @Test
    void create_IndexNotFoundException() {
        // given
        // when
        final Executable executable = directoryReaderFactory::create;
        // then
        assertThrows(IndexNotFoundException.class, executable);
    }


    @Test
    void create_firstTime() {
        // given
        createIndex();
        // when
        final DirectoryReader reader = directoryReaderFactory.create();
        // then
        assertNotNull(reader);
    }


    @Test
    void create_reopen() {
        // given
        createIndex();
        final DirectoryReader oldReader = directoryReaderFactory.create();
        createIndex();
        // when
        final DirectoryReader reader = directoryReaderFactory.create();
        // then
        assertNotNull(reader);
        assertNotSame(oldReader, reader);
    }


    @SneakyThrows
    private void createIndex() {
        final IndexWriter indexWriter = new IndexWriter(directory, new IndexWriterConfig());
        try (indexWriter) {
            indexWriter.addDocument(new Document());
            indexWriter.commit();
        }
    }

}