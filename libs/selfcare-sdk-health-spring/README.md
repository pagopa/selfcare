# Selfcare Spring Health SDK

Spring Boot 3.5 and Java 17 readiness helpers backed by Spring Boot Actuator.
This artifact is independent from the Quarkus `selfcare-sdk-health` module.

## Usage

Add the library and `spring-boot-starter-actuator` to the consuming service, then implement an
indicator as a Spring bean:

```java
@Component("mongoReadiness")
public class MongoReadinessIndicator extends AbstractMongoReadinessIndicator {

    private final MongoClient client;
    private final String database;
    private final String host;

    public MongoReadinessIndicator(
            MongoClient client,
            @Value("${spring.data.mongodb.database}") String database,
            @Value("${spring.data.mongodb.uri}") String connectionString) {
        this.client = client;
        this.database = database;
        this.host = hostFromConnectionString(connectionString);
    }

    @Override
    protected String checkName() {
        return "mongodb";
    }

    @Override
    protected String databaseName() {
        return database;
    }

    @Override
    protected String host() {
        return host;
    }

    @Override
    protected void probe() {
        client.getDatabase(database).runCommand(new Document("ping", 1));
    }
}
```

Include the bean in the Actuator readiness group:

```properties
management.endpoint.health.group.readiness.include=readinessState,mongoReadiness
```

The indicator reports `UP` or `DOWN` with stable `latencyMs`, `error`, and dependency-specific
details. Override `timeout()` when the default two-second timeout is unsuitable.
