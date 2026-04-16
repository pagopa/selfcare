package it.pagopa.selfcare.party.registry_proxy.connector.lucene.reader;

import it.pagopa.selfcare.party.registry_proxy.connector.api.IndexSearchService;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Entity;
import it.pagopa.selfcare.party.registry_proxy.connector.model.QueryFilter;
import it.pagopa.selfcare.party.registry_proxy.connector.model.QueryResult;
import it.pagopa.selfcare.party.registry_proxy.connector.model.SearchField;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.owasp.encoder.Encode;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Slf4j
abstract class IndexSearchServiceTemplate<T> implements IndexSearchService<T> {

    private static final String FIELD_IS_REQUIRED = "A search field is required";
    private static final String VALUE_IS_REQUIRED = "A search value is required";
    private static final String FULL_TEXT_SEARCH_START = "fullTextSearch start";
    private static final String FULL_TEXT_SEARCH_RESULT = "fullTextSearch result = {}";
    private static final String FULL_TEXT_SEARCH_END = "fullTextSearch end";

    private final DirectoryReaderFactory directoryReaderFactory;
    private final Analyzer analyzer;
    private final Function<Document, T> documentConverter;


    protected IndexSearchServiceTemplate(Directory directory, Analyzer analyzer, Function<Document, T> documentConverter) {
        log.trace("Initializing {}", getClass().getSimpleName());
        directoryReaderFactory = new DirectoryReaderFactory(directory);
        this.analyzer = analyzer;
        this.documentConverter = documentConverter;
    }


    @SneakyThrows
    @Override
    public QueryResult<T> fullTextSearch(SearchField field, String value, int page, int limit) {
        log.trace(FULL_TEXT_SEARCH_START);
        log.debug("fullTextSearch field = {}, value = {}, page = {}, limit = {}", field, value, page, limit);
        Assert.notNull(field, FIELD_IS_REQUIRED);
        Assert.notNull(value, VALUE_IS_REQUIRED);
        Assert.isTrue(page > 0, "A page number must be great than 0");
        Assert.isTrue(limit > 0, "A limit result must be great than or equal to 1");
        final DirectoryReader reader = directoryReaderFactory.create();
        final IndexSearcher indexSearcher = new IndexSearcher(reader);
        final TopScoreDocCollector collector = TopScoreDocCollector.create(reader.numDocs(), Integer.MAX_VALUE);
        final QueryParser parser = new QueryParser(field.toString(), analyzer);
        parser.setPhraseSlop(4);
        indexSearcher.search(parser.parse(value), collector);
        final TopDocs hits = collector.topDocs((page - 1) * limit, limit);

        final List<T> categories = new ArrayList<>(hits.scoreDocs.length);
        for (ScoreDoc scoreDoc : hits.scoreDocs) {
            categories.add(documentConverter.apply(indexSearcher.doc(scoreDoc.doc)));
        }

        final QueryResult<T> queryResult = getQueryResult(categories, hits.totalHits.value);
        log.debug(FULL_TEXT_SEARCH_RESULT, queryResult);
        log.trace(FULL_TEXT_SEARCH_END);
        return queryResult;
    }

    @SneakyThrows
    @Override
    public QueryResult<T> fullTextSearch(SearchField searchFieldDescription,
                                         String currentDescription,
                                         SearchField searchFieldCategory,
                                         String currentCategories,
                                         int page,
                                         int limit) {
        log.trace(FULL_TEXT_SEARCH_START);

        log.debug("fullTextSearch field = {}, value = {}, filter = {}, categories = {}, page = {}, limit = {}",
                searchFieldDescription, Encode.forJava(currentDescription), searchFieldCategory, Encode.forJava(currentCategories), page, limit);

        Assert.notNull(searchFieldDescription, FIELD_IS_REQUIRED);
        Assert.notNull(currentDescription, VALUE_IS_REQUIRED);
        Assert.notNull(searchFieldCategory, FIELD_IS_REQUIRED);
        Assert.notNull(currentCategories, VALUE_IS_REQUIRED);
        Assert.isTrue(page > 0, "A page number must be greater than 0");
        Assert.isTrue(limit > 0, "A limit result must be greater than or equal to 1");

        final DirectoryReader reader = directoryReaderFactory.create();
        final IndexSearcher indexSearcher = new IndexSearcher(reader);

        final TopScoreDocCollector collector =
                TopScoreDocCollector.create(reader.numDocs(), Integer.MAX_VALUE);

        QueryParser descriptionParser = new QueryParser(searchFieldDescription.toString(), analyzer);
        descriptionParser.setPhraseSlop(4);
        Query descriptionQuery = descriptionParser.parse(currentDescription);

        BooleanQuery.Builder categoryQueryBuilder = new BooleanQuery.Builder();

        for (String category : currentCategories.split(",")) {
            String trimmedCategory = category.trim();
            if (!trimmedCategory.isEmpty()) {
                Query categoryQuery = new TermQuery(new Term(searchFieldCategory.toString(), trimmedCategory));
                categoryQueryBuilder.add(categoryQuery, BooleanClause.Occur.SHOULD);
            }
        }

        Query categoriesQuery = categoryQueryBuilder.build();
        Query boostedDescriptionQuery = new BoostQuery(descriptionQuery, 5.0f);

        BooleanQuery.Builder finalQueryBuilder = new BooleanQuery.Builder();
        finalQueryBuilder.add(categoriesQuery, BooleanClause.Occur.MUST);
        finalQueryBuilder.add(boostedDescriptionQuery, BooleanClause.Occur.SHOULD);

        Query finalQuery = finalQueryBuilder.build();

        indexSearcher.search(finalQuery, collector);

        final TopDocs hits = collector.topDocs((page - 1) * limit, limit);

        final List<T> results = new ArrayList<>(hits.scoreDocs.length);

        for (ScoreDoc scoreDoc : hits.scoreDocs) {
            results.add(documentConverter.apply(indexSearcher.doc(scoreDoc.doc)));
        }

        final QueryResult<T> queryResult = getQueryResult(results, hits.totalHits.value);
        log.debug(FULL_TEXT_SEARCH_RESULT, queryResult);
        log.trace(FULL_TEXT_SEARCH_END);

        return queryResult;
    }


    @SneakyThrows
    @Override
    public List<T> findById(SearchField field, String value) {
        log.trace("findById start");
        log.debug("findById field = {}, value = {}", field, value);
        Assert.notNull(field, FIELD_IS_REQUIRED);
        Assert.notNull(value, VALUE_IS_REQUIRED);
        final DirectoryReader reader = directoryReaderFactory.create();
        final IndexSearcher indexSearcher = new IndexSearcher(reader);
        final TermQuery query = new TermQuery(new Term(field.toString(), value));
        final TopDocs hits = indexSearcher.search(query, 1);

        final List<T> items = new ArrayList<>(hits.scoreDocs.length);
        for (ScoreDoc scoreDoc : hits.scoreDocs) {
            items.add(documentConverter.apply(indexSearcher.doc(scoreDoc.doc)));
        }

        log.debug("findById result = {}", items);
        log.trace("findById end");
        return items;
    }



    @SneakyThrows
    @Override
    public QueryResult<T> findAll(int page, int limit, String entityType, QueryFilter... filters) {
        log.trace("findAll start");
        log.debug("findAll page = {}, limit = {}, filters = {}", page, limit, filters);
        final DirectoryReader reader = directoryReaderFactory.create();
        final IndexSearcher indexSearcher = new IndexSearcher(reader);
        final Query query;

        final BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();
        final TermQuery indexFilterQuery = new TermQuery(new Term(Entity.ENTITY_TYPE.toString(), entityType));
        queryBuilder.add(indexFilterQuery, BooleanClause.Occur.MUST);

        if (filters != null && filters.length != 0) {
            for (QueryFilter filter : filters) {
                Assert.notNull(filter.getField(), FIELD_IS_REQUIRED);
                Assert.notNull(filter.getValue(), VALUE_IS_REQUIRED);
                final TermQuery termQuery = new TermQuery(new Term(filter.getField().toString(), filter.getValue()));
                queryBuilder.add(termQuery, BooleanClause.Occur.MUST);
            }
        }
        query = queryBuilder.build();

        final TopScoreDocCollector collector = TopScoreDocCollector.create(reader.numDocs(), reader.numDocs());
        indexSearcher.search(query, collector);

        final TopDocs hits = collector.topDocs((page - 1) * limit, limit);

        final List<T> items = new ArrayList<>(hits.scoreDocs.length);
        for (ScoreDoc scoreDoc : hits.scoreDocs) {
            items.add(documentConverter.apply(indexSearcher.doc(scoreDoc.doc)));
        }

        final QueryResult<T> queryResult = getQueryResult(items, hits.totalHits.value);
        log.debug("findAll result = {}", queryResult);
        log.trace("findAll end");
        return queryResult;
    }

    protected abstract QueryResult<T> getQueryResult(List<T> items, long totalHits);



}
