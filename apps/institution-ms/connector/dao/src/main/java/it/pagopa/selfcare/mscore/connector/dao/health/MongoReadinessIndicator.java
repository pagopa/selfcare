package it.pagopa.selfcare.mscore.connector.dao.health;

import it.pagopa.selfcare.commons.health.spring.AbstractMongoReadinessIndicator;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Component("mongoReadiness")
public class MongoReadinessIndicator extends AbstractMongoReadinessIndicator {

    private final MongoTemplate mongoTemplate;
    private final String databaseName;
    private final String host;

    public MongoReadinessIndicator(
            MongoTemplate mongoTemplate,
            @Value("${spring.data.mongodb.database}") String databaseName,
            @Value("${spring.data.mongodb.uri}") String connectionString) {
        this.mongoTemplate = mongoTemplate;
        this.databaseName = databaseName;
        this.host = hostFromConnectionString(connectionString);
    }

    @Override
    protected String checkName() {
        return "mongodb-institution";
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
    protected void probe() {
        mongoTemplate.executeCommand(new Document("ping", 1));
    }
}
