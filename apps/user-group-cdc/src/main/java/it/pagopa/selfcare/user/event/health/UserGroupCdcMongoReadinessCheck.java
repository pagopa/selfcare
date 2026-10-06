package it.pagopa.selfcare.user.event.health;

import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.commons.health.AbstractMongoReadinessCheck;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.Document;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.health.Readiness;

@Readiness
@ApplicationScoped
public class UserGroupCdcMongoReadinessCheck extends AbstractMongoReadinessCheck {

    private final ReactiveMongoClient mongoClient;
    private final String databaseName;
    private final String host;

    @Inject
    public UserGroupCdcMongoReadinessCheck(
            ReactiveMongoClient mongoClient,
            @ConfigProperty(name = "quarkus.mongodb.database") String databaseName,
            @ConfigProperty(name = "quarkus.mongodb.connection-string") String connectionString) {
        this.mongoClient = mongoClient;
        this.databaseName = databaseName;
        this.host = hostFromConnectionString(connectionString);
    }

    @Override
    protected String checkName() {
        return "mongodb-user-group-cdc";
    }

    @Override
    protected String databaseName() {
        return databaseName;
    }

    @Override
    protected String host() {
        return host;
    }

    @Override
    protected Uni<?> probe() {
        return mongoClient.getDatabase(databaseName).runCommand(new Document("ping", 1));
    }
}
