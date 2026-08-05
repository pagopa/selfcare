package it.pagopa.selfcare.commons.health;

import java.util.HashMap;
import java.util.Map;

/**
 * Base class for readiness checks targeting a <b>MongoDB</b> instance.
 *
 * <p>The Quarkus {@code quarkus-mongodb-client} extension already auto-registers
 * {@code io.quarkus.mongodb.health.MongoHealthCheck}, which runs {@code listDatabaseNames()} and
 * is usually enough. Use this base class when a stricter check is required (e.g. a per-database
 * {@code ping} command executed on the actual database used by the application), or when
 * finer-grained {@code data} enrichment is desired.
 *
 * <p><b>What "ping" does.</b> The recommended probe below runs the MongoDB
 * <a href="https://www.mongodb.com/docs/manual/reference/command/ping/"><code>ping</code></a>
 * admin command. It does <b>not</b> create, read, update or delete any document; it only asks
 * the server "are you alive?" and expects a {@code {ok: 1}} reply. In the driver API the command
 * payload is expressed as a BSON document, hence the {@code new Document("ping", 1)} argument —
 * that {@link org.bson.Document} is the <i>command</i> being sent, <b>not</b> a document inserted
 * into the database.
 *
 * <p>Example:
 * <pre>{@code
 * @Readiness
 * @ApplicationScoped
 * public class OnboardingMongoReadinessCheck extends AbstractMongoReadinessCheck {
 *
 *     @Inject ReactiveMongoClient client;
 *     @ConfigProperty(name = "quarkus.mongodb.database") String database;
 *
 *     @Override protected String checkName()    { return "mongodb-onboarding"; }
 *     @Override protected String databaseName() { return database; }
 *
 *     @Override
 *     protected Uni<?> probe() {
 *         // Sends the {ping: 1} admin command; nothing is written to the DB.
 *         return client.getDatabase(databaseName())
 *                 .runCommand(new org.bson.Document("ping", 1));
 *     }
 * }
 * }</pre>
 */
public abstract class AbstractMongoReadinessCheck extends AbstractAsyncReadinessCheck {

    /** Database name against which the {@code ping} (or equivalent) is executed. */
    protected abstract String databaseName();

    @Override
    protected Map<String, String> data() {
        final Map<String, String> data = new HashMap<>(2);
        data.put(HealthCheckConstants.DATA_KEY_COMPONENT, "mongodb");
        data.put(HealthCheckConstants.DATA_KEY_MONGO_DATABASE, databaseName());
        return data;
    }
}

