package it.pagopa.selfcare.party.registry_proxy.connector.lucene.writer;

import it.pagopa.selfcare.party.registry_proxy.connector.lucene.analysis.UOTokenAnalyzer;
import it.pagopa.selfcare.party.registry_proxy.connector.lucene.config.InMemoryIndexConfig;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        InMemoryIndexConfig.class,
        UOTokenAnalyzer.class,
        UOIndexWriterFactory.class
})
class UOIndexWriterFactoryTest {

    @Autowired
    private Directory uosDirectory;

    @Autowired
    private UOIndexWriterFactory indexWriterFactory;


    @Test
    void create() {
        // given
        // when
        final IndexWriter indexWriter = indexWriterFactory.create();
        // then
        assertNotNull(indexWriter);
        assertTrue(indexWriter.isOpen());
        assertSame(uosDirectory, indexWriter.getDirectory());
        assertEquals(UOTokenAnalyzer.class, indexWriter.getAnalyzer().getClass());
    }


    @Test
    void create_Exception() {
        // given
        final IndexWriterFactory indexWriterFactory = new UOIndexWriterFactory(null, new UOTokenAnalyzer());
        // when
        final Executable executable = indexWriterFactory::create;
        // then
        assertThrows(RuntimeException.class, executable);
    }

}