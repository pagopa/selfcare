package it.pagopa.selfcare.onboarding.health;

import com.mongodb.ConnectionString;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.commons.health.AbstractMongoReadinessCheck;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.Document;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.health.Readiness;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Readiness
@ApplicationScoped
public class OnboardingMongoReadinessCheck extends AbstractMongoReadinessCheck {

    private static final String HOST_NOT_AVAILABLE = "n/a";

    private final ReactiveMongoClient mongoClient;
    private final String databaseName;
    private final String host;

    @Inject
    public OnboardingMongoReadinessCheck(
            ReactiveMongoClient mongoClient,
            @ConfigProperty(name = "quarkus.mongodb.database") String databaseName,
            @ConfigProperty(name = "quarkus.mongodb.connection-string") String connectionString) {
        this.mongoClient = mongoClient;
        this.databaseName = databaseName;
        this.host = extractHosts(connectionString);
    }

    @Override
    protected String checkName() {
        return "mongodb-onboarding";
    }

    @Override
    protected String databaseName() {
        return databaseName;
    }

    @Override
    protected Map<String, String> data() {
        final Map<String, String> data = new HashMap<>(super.data());
        data.put("host", host);
        return data;
    }

    @Override
    protected Uni<?> probe() {
        return mongoClient.getDatabase(databaseName)
                .runCommand(new Document("ping", 1));
    }

    private static String extractHosts(String connectionString) {
        try {
            List<String> hosts = new ConnectionString(connectionString).getHosts();
            return hosts.isEmpty() ? HOST_NOT_AVAILABLE : String.join(",", hosts);
        } catch (RuntimeException e) {
            return HOST_NOT_AVAILABLE;
        }
    }
}
