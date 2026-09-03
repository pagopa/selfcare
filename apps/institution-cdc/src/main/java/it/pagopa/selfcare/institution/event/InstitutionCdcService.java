package it.pagopa.selfcare.institution.event;

import com.azure.data.tables.TableClient;
import com.azure.data.tables.models.TableEntity;
import com.azure.data.tables.models.TableServiceException;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.FullDocument;
import io.quarkus.mongodb.ChangeStreamOptions;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.mongodb.reactive.ReactiveMongoCollection;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.Startup;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.smallrye.mutiny.Multi;
import it.pagopa.selfcare.institution.event.entity.InstitutionEntity;
import it.pagopa.selfcare.institution.event.internalevents.InternalEvents;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.bson.BsonDocument;
import org.bson.conversions.Bson;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static com.mongodb.client.model.Projections.fields;
import static com.mongodb.client.model.Projections.include;
import static java.util.Arrays.asList;

@Startup
@Slf4j
@ApplicationScoped
public class InstitutionCdcService {

    public static final String COLLECTION_NAME = "Institution";
    public static final String CDC_START_AT_PARTITION_KEY = "Institution";
    public static final String CDC_START_AT_ROW_KEY = "0001";
    public static final String CDC_START_AT_PROPERTY = "startAt";

    private final TableClient tableClient;
    private final ReactiveMongoClient mongoClient;

    private final InternalEvents internalEvents;

    public InstitutionCdcService(TableClient tableClient,
                                 ReactiveMongoClient mongoClient,
                                 InternalEvents internalEvents,
                                 @ConfigProperty(name = "quarkus.mongodb.database") String mongodbDatabase) {
        this.tableClient = tableClient;
        this.mongoClient = mongoClient;
        this.internalEvents = internalEvents;
        initOrderStream(mongodbDatabase);
    }

    private void initOrderStream(String mongodbDatabase) {
        log.info("Starting initOrderStream ... ");

        //Retrieve last resumeToken for watching collection at specific operation
        String resumeToken = null;

        if (!ConfigUtils.getProfiles().contains("test")) {
            try {
                TableEntity cdcStartAtEntity = tableClient.getEntity(CDC_START_AT_PARTITION_KEY, CDC_START_AT_ROW_KEY);
                if (Objects.nonNull(cdcStartAtEntity))
                    resumeToken = (String) cdcStartAtEntity.getProperty(CDC_START_AT_PROPERTY);
            } catch (TableServiceException e) {
                log.warn("Table StarAt not found, it is starting from now ...");
            }
        }

        // Initialize watching collection
        ReactiveMongoCollection<InstitutionEntity> dataCollection = mongoClient
                .getDatabase(mongodbDatabase)
                .getCollection(COLLECTION_NAME, InstitutionEntity.class);
        ChangeStreamOptions options = new ChangeStreamOptions()
                .fullDocument(FullDocument.UPDATE_LOOKUP);
        if (Objects.nonNull(resumeToken))
            options = options.resumeAfter(BsonDocument.parse(resumeToken));

        Bson match = Aggregates.match(Filters.in("operationType", asList("update", "replace", "insert")));
        Bson project = Aggregates.project(fields(include("_id", "ns", "documentKey", "fullDocument")));
        List<Bson> pipeline = Arrays.asList(match, project);

        Multi<ChangeStreamDocument<InstitutionEntity>> publisher = dataCollection.watch(pipeline, InstitutionEntity.class, options);

        publisher.subscribe().with(
                this::consumerInstitutionRepositoryEvent,
                failure -> {
                    log.error("Error during subscribe collection, exception: {} , message: {}", failure.toString(), failure.getMessage());
                    Quarkus.asyncExit();
                });
        log.info("Completed initOrderStream ... ");
    }

    public void consumerInstitutionRepositoryEvent(ChangeStreamDocument<InstitutionEntity> document) {
        assert document.getDocumentKey() != null;
        assert document.getFullDocument() != null;
        internalEvents.sendInstitutionUpdatedEvent(document.getFullDocument());
    }

}
