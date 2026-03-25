package it.pagopa.selfcare.party.registry_proxy.connector.lucene.writer;

import it.pagopa.selfcare.party.registry_proxy.connector.lucene.analysis.StationTokenAnalyzer;
import it.pagopa.selfcare.party.registry_proxy.connector.lucene.config.InMemoryIndexConfig;
import it.pagopa.selfcare.party.registry_proxy.connector.lucene.model.DummyStation;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Entity;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Station;
import lombok.SneakyThrows;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.store.Directory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        InMemoryIndexConfig.class,
        StationTokenAnalyzer.class,
        StationIndexWriterFactory.class,
        StationIndexWriterService.class
})
class StationIndexWriterServiceTest {

    @Autowired
    @Qualifier("anacDirectory")
    private Directory directory;

    @Autowired
    private StationIndexWriterService indexWriterService;

    @SneakyThrows
    private long count(Directory directory) {
        final DirectoryReader reader = DirectoryReader.open(this.directory);
        final IndexSearcher indexSearcher = new IndexSearcher(reader);
        final MatchAllDocsQuery query = new MatchAllDocsQuery();
        return indexSearcher.count(query);
    }

    @Test
    void adds() {
        // given
        final List<? extends Station> items = List.of(new DummyStation());
        // when
        indexWriterService.adds(items);
        // then
        assertEquals(1, count(directory));
    }

    @Test
    void deleteAll() {
        // given
        // when
        indexWriterService.deleteAll();
        // then
        assertEquals(0, count(directory));
    }

    @Test
    void cleanIndex() {
        // given
        // when
        indexWriterService.cleanIndex(Entity.STATION.name());
        // then
        assertEquals(0, count(directory));
    }

}