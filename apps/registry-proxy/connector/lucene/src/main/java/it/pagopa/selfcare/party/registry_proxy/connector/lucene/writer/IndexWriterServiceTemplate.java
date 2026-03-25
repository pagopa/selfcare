package it.pagopa.selfcare.party.registry_proxy.connector.lucene.writer;

import it.pagopa.selfcare.party.registry_proxy.connector.api.IndexWriterService;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Entity;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Institution.Field;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.TermQuery;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Slf4j
abstract class IndexWriterServiceTemplate<T> implements IndexWriterService<T> {

    private final Function<T, Document> documentConverter;
    private final IndexWriterFactory indexWriterFactory;

    public IndexWriterServiceTemplate(IndexWriterFactory indexWriterFactory, Function<T, Document> documentConverter) {
        log.trace("Initializing {}", getClass().getSimpleName());
        this.indexWriterFactory = indexWriterFactory;
        this.documentConverter = documentConverter;
    }

    @SneakyThrows
    @Override
    public void adds(List<? extends T> items) {
        log.trace("adds start");
        log.debug("adds items = {}", items.size());
        final IndexWriter indexWriter = indexWriterFactory.create();
        try (indexWriter) {
            for (T item : items) {
                final Document doc = documentConverter.apply(item);
                indexWriter.updateDocument(new Term(Field.ID.toString(), getId(item)), doc);
            }
            indexWriter.commit();
        }
        log.trace("adds end");
    }

    @SneakyThrows
    @Override
    public void deleteAll() {
        log.trace("deleteAll start");
        final IndexWriter indexWriter = indexWriterFactory.create();
        try (indexWriter) {
            indexWriter.deleteAll();
            indexWriter.commit();
        }
        log.trace("deleteAll end");
    }

    @SneakyThrows
    @Override
    public void updateDocumentValues(T item, Map<String, String> fieldsToUpdate) {
        log.trace("updateDocumentValues start");
        final IndexWriter indexWriter = indexWriterFactory.create();
        try (indexWriter) {
            final String fieldId = getId(item);
            final Document document = documentConverter.apply(item);
            for (Map.Entry<String, String> entry : fieldsToUpdate.entrySet()) {
                document.add(new StringField(entry.getKey(), entry.getValue(), org.apache.lucene.document.Field.Store.YES));
            }
            indexWriter.updateDocument(new Term(Field.ID.toString(), fieldId), document);
            indexWriter.commit();
            log.debug("Document for {} with ID {} has been updated", item.getClass(), fieldId);
        }
        log.trace("updateDocumentValues end");
    }

    @SneakyThrows
    @Override
    public void cleanIndex(String entityType) {
        log.trace("cleanIndex start");
        final IndexWriter indexWriter = indexWriterFactory.create();
        try (indexWriter) {
            final BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();
            final TermQuery indexFilterQuery = new TermQuery(new Term(Entity.ENTITY_TYPE.toString(), entityType));
            queryBuilder.add(indexFilterQuery, BooleanClause.Occur.MUST);
            indexWriter.deleteDocuments(queryBuilder.build());
            indexWriter.commit();
        }
        log.trace("cleanIndex end");
    }

    protected abstract String getId(T item);

}
