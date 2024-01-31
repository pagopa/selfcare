package it.pagopa.selfcare.user.event;

import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.FullDocument;
import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.mongodb.ChangeStreamOptions;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.mongodb.reactive.ReactiveMongoCollection;
import io.quarkus.runtime.Startup;
import io.smallrye.mutiny.Multi;
import it.pagopa.selfcare.user.event.constant.OnboardedProductState;
import it.pagopa.selfcare.user.event.entity.UserInstitution;
import it.pagopa.selfcare.user.event.mapper.UserMapper;
import it.pagopa.selfcare.user.event.repository.UserInstitutionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.bson.conversions.Bson;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.*;

import static com.mongodb.client.model.Projections.fields;
import static com.mongodb.client.model.Projections.include;
import static java.util.Arrays.asList;

@Startup
@Slf4j
@ApplicationScoped
@IfBuildProperty(name = "user-cdc.mongodb.watch.enabled", stringValue = "true")
public class UserInstitutionCdcService {
    private static final String COLLECTION_NAME = "userInstitutions";
    private static final List<OnboardedProductState> VALID_PRODUCT_STATE = List.of(OnboardedProductState.ACTIVE, OnboardedProductState.PENDING, OnboardedProductState.TOBEVALIDATED);

    private final String mongodbDatabase;
    private final ReactiveMongoClient mongoClient;
    private final UserMapper userMapper;
    private final UserInstitutionRepository userInstitutionRepository;

    public UserInstitutionCdcService(ReactiveMongoClient mongoClient,
                                     @ConfigProperty(name = "quarkus.mongodb.database") String mongodbDatabase,
                                     UserMapper userMapper,
                                     UserInstitutionRepository userInstitutionRepository) {
        this.mongoClient = mongoClient;
        this.mongodbDatabase = mongodbDatabase;
        this.userMapper = userMapper;
        this.userInstitutionRepository = userInstitutionRepository;
        initOrderStream();
    }

    private void initOrderStream() {
        ReactiveMongoCollection<UserInstitution> dataCollection = getCollection();
        ChangeStreamOptions options = new ChangeStreamOptions().fullDocument(FullDocument.UPDATE_LOOKUP);

        Bson match = Aggregates.match(Filters.in("operationType", asList("update", "replace", "insert")));
        Bson project = Aggregates.project(fields(include("_id", "ns", "documentKey", "fullDocument")));
        List<Bson> pipeline = Arrays.asList(match, project);

        Multi<ChangeStreamDocument<UserInstitution>> publisher = dataCollection.watch(pipeline, UserInstitution.class, options);
        publisher.subscribe().with(this::consumerUserInstitutionRepositoryEvent);
    }

    private ReactiveMongoCollection<UserInstitution> getCollection() {
        return mongoClient
                .getDatabase(mongodbDatabase)
                .getCollection(COLLECTION_NAME, UserInstitution.class);
    }

    protected void consumerUserInstitutionRepositoryEvent(ChangeStreamDocument<UserInstitution> document) {
        assert document.getFullDocument() != null;
        userInstitutionRepository.updateUser(document.getFullDocument())
                .subscribe().with(
                        result -> log.info("UserInfo collection successfully updated"),
                        failure -> log.error("Error during UserInfo collection updating, message:" + failure.getMessage()));

    }
}
