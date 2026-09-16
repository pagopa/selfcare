package it.pagopa.selfcare.tenant.mongodb;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.reactivestreams.client.MongoClients;
import io.quarkus.arc.Unremovable;
import io.quarkus.mongodb.impl.ReactiveMongoClientImpl;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.mongodb.reactive.ReactiveMongoCollection;
import io.quarkus.mongodb.reactive.ReactiveMongoDatabase;
import io.quarkus.mongodb.runtime.MongoClientSupport;
import it.pagopa.selfcare.tenant.TenantContext;
import it.pagopa.selfcare.tenant.TenantRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.ClassModel;
import org.bson.codecs.pojo.Conventions;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.codecs.pojo.PropertyCodecProvider;

/**
 * Creates Mongo clients from the tenant registry and replaces the Panache default
 * {@link ReactiveMongoClient}. Do not set {@code @MongoEntity(clientName)}: Panache
 * would also create a synthetic named client and CDI would become ambiguous.
 */
@Singleton
@Unremovable
public class TenantMongoClientProducer {

    private final TenantRegistry tenantRegistry;
    private final TenantContext tenantContext;
    private final MongoClientSupport mongoClientSupport;
    private final Instance<CodecProvider> codecProviders;
    private final Instance<PropertyCodecProvider> propertyCodecProviders;
    private Map<String, ReactiveMongoClient> clients;

    @Inject
    public TenantMongoClientProducer(
            TenantRegistry tenantRegistry,
            TenantContext tenantContext,
            MongoClientSupport mongoClientSupport,
            Instance<CodecProvider> codecProviders,
            Instance<PropertyCodecProvider> propertyCodecProviders) {
        this.tenantRegistry = tenantRegistry;
        this.tenantContext = tenantContext;
        this.mongoClientSupport = mongoClientSupport;
        this.codecProviders = codecProviders;
        this.propertyCodecProviders = propertyCodecProviders;
    }

    @PostConstruct
    void initialize() {
        Map<String, ReactiveMongoClient> initializedClients = new HashMap<>();
        try {
            tenantRegistry.supportedTenantIds().forEach(tenantId -> {
                String connectionString = tenantRegistry.connectionString(tenantId)
                        .orElseThrow(() -> new IllegalStateException(
                                "Missing Mongo connection string for tenant " + tenantId));
                initializedClients.put(tenantId, createClient(connectionString));
            });
            clients = Map.copyOf(initializedClients);
        } catch (RuntimeException exception) {
            initializedClients.values().forEach(ReactiveMongoClient::close);
            throw exception;
        }
    }

    private ReactiveMongoClient createClient(String connectionString) {
        MongoClientSettings.Builder settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionString));
        settings.codecRegistry(panacheCodecRegistry());
        return new ReactiveMongoClientImpl(MongoClients.create(settings.build()));
    }

    /**
     * Same codec setup as Quarkus {@code MongoClients}: default registry plus automatic
     * POJO mapping so Panache entities such as {@code Onboarding} can be encoded.
     */
    private CodecRegistry panacheCodecRegistry() {
        List<CodecProvider> providers = new ArrayList<>();
        codecProviders.forEach(providers::add);

        PojoCodecProvider.Builder pojoCodecProviderBuilder = PojoCodecProvider.builder()
                .automatic(true)
                .conventions(Conventions.DEFAULT_CONVENTIONS);
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        for (String bsonDiscriminator : mongoClientSupport.getBsonDiscriminators()) {
            try {
                pojoCodecProviderBuilder.register(
                        ClassModel.builder(Class.forName(bsonDiscriminator, true, classLoader))
                                .enableDiscriminator(true)
                                .build());
            } catch (ClassNotFoundException ignored) {
                // Quarkus ignores missing discriminator classes as well.
            }
        }
        propertyCodecProviders.forEach(pojoCodecProviderBuilder::register);

        CodecRegistry defaultCodecRegistry = MongoClientSettings.getDefaultCodecRegistry();
        if (providers.isEmpty()) {
            return fromRegistries(defaultCodecRegistry, fromProviders(pojoCodecProviderBuilder.build()));
        }
        return fromRegistries(
                fromProviders(providers),
                defaultCodecRegistry,
                fromProviders(pojoCodecProviderBuilder.build()));
    }

    @PreDestroy
    void closeClients() {
        if (clients != null) {
            clients.values().forEach(ReactiveMongoClient::close);
        }
    }

    /**
     * Panache resolves the default client with {@code Arc.instance(Default)}.
     * {@link Alternative} {@link Priority} wins over the Quarkus synthetic client.
     */
    @Produces
    @Alternative
    @Priority(1)
    @Singleton
    @Unremovable
    public ReactiveMongoClient tenantAwareClient() {
        return (ReactiveMongoClient) Proxy.newProxyInstance(
                ReactiveMongoClient.class.getClassLoader(),
                new Class<?>[]{ReactiveMongoClient.class},
                this::invokeOnTenantClient);
    }

    public ReactiveMongoClient clientForTenant(String tenantId) {
        ReactiveMongoClient client =
                clients.get(tenantRegistry.normalizeTenantId(tenantId));
        if (client == null) {
            throw new IllegalStateException("Unsupported tenant: " + tenantId);
        }
        return client;
    }

    public ReactiveMongoDatabase databaseForCurrentTenant() {
        String tenantId = tenantContext.requiredTenantId();
        return clientForTenant(tenantId)
                .getDatabase(tenantRegistry.resolve(tenantId).mongo().database());
    }

    public <T> ReactiveMongoCollection<T> collection(String name, Class<T> entityType) {
        return databaseForCurrentTenant().getCollection(name, entityType);
    }

    private Object invokeOnTenantClient(Object proxy, Method method, Object[] arguments)
            throws Throwable {
        if ("close".equals(method.getName())) {
            return null;
        }
        if ("toString".equals(method.getName())) {
            return "TenantAwareReactiveMongoClient";
        }
        if ("hashCode".equals(method.getName())) {
            return System.identityHashCode(proxy);
        }
        if ("equals".equals(method.getName())) {
            return proxy == arguments[0];
        }
        String tenantId = tenantContext.requiredTenantId();
        if ("getDatabase".equals(method.getName())) {
            String databaseName = arguments != null
                    && arguments.length == 1
                    && arguments[0] instanceof String name
                    && !name.isBlank()
                    ? name
                    : tenantRegistry.resolve(tenantId).mongo().database();
            return clientForTenant(tenantId).getDatabase(databaseName);
        }
        return method.invoke(clientForTenant(tenantId), arguments);
    }
}
