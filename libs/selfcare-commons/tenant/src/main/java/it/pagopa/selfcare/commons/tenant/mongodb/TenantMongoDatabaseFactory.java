package it.pagopa.selfcare.commons.tenant.mongodb;

import com.mongodb.ClientSessionOptions;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import it.pagopa.selfcare.commons.tenant.TenantContext;
import it.pagopa.selfcare.commons.tenant.TenantDefinition;
import it.pagopa.selfcare.commons.tenant.TenantRegistry;
import jakarta.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.bson.codecs.configuration.CodecRegistry;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

public class TenantMongoDatabaseFactory implements MongoDatabaseFactory {

    private final TenantContext tenantContext;
    private final Map<String, MongoClient> clients = new ConcurrentHashMap<>();
    private final Map<String, MongoDatabaseFactory> factories = new ConcurrentHashMap<>();

    public TenantMongoDatabaseFactory(TenantRegistry tenantRegistry, TenantContext tenantContext) {
        this.tenantContext = tenantContext;
        tenantRegistry.supportedTenantIds().forEach(tenantId -> {
            TenantDefinition.MongoDefinition mongo = tenantRegistry.resolve(tenantId).mongo();
            String connectionString = tenantRegistry.mongoConnectionString(tenantId)
                    .orElseThrow(() -> new IllegalStateException(
                            "Missing Mongo connection string for tenant " + tenantId));
            MongoClient client = MongoClients.create(connectionString);
            clients.put(tenantId, client);
            factories.put(
                    tenantId,
                    new SimpleMongoClientDatabaseFactory(client, mongo.database()));
        });
    }

    @Override
    public MongoDatabase getMongoDatabase() {
        return currentFactory().getMongoDatabase();
    }

    @Override
    public MongoDatabase getMongoDatabase(String ignoredDatabaseName) {
        return currentFactory().getMongoDatabase();
    }

    @Override
    public PersistenceExceptionTranslator getExceptionTranslator() {
        return contextualOrAnyFactory().getExceptionTranslator();
    }

    @Override
    public CodecRegistry getCodecRegistry() {
        return contextualOrAnyFactory().getCodecRegistry();
    }

    @Override
    public ClientSession getSession(ClientSessionOptions options) {
        return currentFactory().getSession(options);
    }

    @Override
    public MongoDatabaseFactory withSession(ClientSessionOptions options) {
        return currentFactory().withSession(options);
    }

    @Override
    public MongoDatabaseFactory withSession(ClientSession session) {
        return currentFactory().withSession(session);
    }

    @Override
    public boolean isTransactionActive() {
        return currentFactory().isTransactionActive();
    }

    private MongoDatabaseFactory currentFactory() {
        String tenantId = tenantContext.requiredTenantId();
        MongoDatabaseFactory factory = factories.get(tenantId);
        if (factory == null) {
            throw new IllegalStateException("Mongo client is not configured for tenant " + tenantId);
        }
        return factory;
    }

    private MongoDatabaseFactory anyFactory() {
        return factories.values().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No Mongo clients are configured"));
    }

    private MongoDatabaseFactory contextualOrAnyFactory() {
        return tenantContext.tenantId()
                .map(factories::get)
                .orElseGet(this::anyFactory);
    }

    @PreDestroy
    void close() {
        clients.values().forEach(MongoClient::close);
    }
}
