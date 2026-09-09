package it.pagopa.selfcare.tenant.mongodb;

import com.mongodb.reactivestreams.client.MongoClients;
import io.quarkus.mongodb.impl.ReactiveMongoClientImpl;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import it.pagopa.selfcare.tenant.TenantContext;
import it.pagopa.selfcare.tenant.TenantRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/**
 * Creates and routes Mongo clients from the shared tenant registry.
 */
@Singleton
public class TenantMongoClientProducer {

    private final TenantRegistry tenantRegistry;
    private final TenantContext tenantContext;
    private Map<String, ReactiveMongoClient> clients;

    @Inject
    public TenantMongoClientProducer(
            TenantRegistry tenantRegistry,
            TenantContext tenantContext) {
        this.tenantRegistry = tenantRegistry;
        this.tenantContext = tenantContext;
    }

    @PostConstruct
    void initialize() {
        Map<String, ReactiveMongoClient> initializedClients = new HashMap<>();
        try {
            tenantRegistry.supportedTenantIds().forEach(tenantId -> {
                String connectionString = tenantRegistry.connectionString(tenantId)
                        .orElseThrow(() -> new IllegalStateException(
                                "Missing Mongo connection string for tenant " + tenantId));
                initializedClients.put(
                        tenantId,
                        new ReactiveMongoClientImpl(MongoClients.create(connectionString)));
            });
            clients = Map.copyOf(initializedClients);
        } catch (RuntimeException exception) {
            initializedClients.values().forEach(ReactiveMongoClient::close);
            throw exception;
        }
    }

    @PreDestroy
    void closeClients() {
        if (clients != null) {
            clients.values().forEach(ReactiveMongoClient::close);
        }
    }

    @Produces
    @Alternative
    @Priority(1)
    @Singleton
    public ReactiveMongoClient tenantAwareClient() {
        InvocationHandler handler = this::invokeOnTenantClient;
        return (ReactiveMongoClient) Proxy.newProxyInstance(
                ReactiveMongoClient.class.getClassLoader(),
                new Class<?>[]{ReactiveMongoClient.class},
                handler);
    }

    public ReactiveMongoClient clientForTenant(String tenantId) {
        ReactiveMongoClient client =
                clients.get(tenantRegistry.normalizeTenantId(tenantId));
        if (client == null) {
            throw new IllegalStateException("Unsupported tenant: " + tenantId);
        }
        return client;
    }

    private Object invokeOnTenantClient(Object proxy, Method method, Object[] arguments)
            throws Throwable {
        if (method.getName().equals("close")) {
            return null;
        }
        if (method.getName().equals("toString")) {
            return "TenantAwareReactiveMongoClient";
        }
        if (method.getName().equals("getDatabase")) {
            String databaseName = tenantRegistry.resolve(tenantContext.requiredTenantId())
                    .mongo()
                    .database();
            return clientForTenant(tenantContext.requiredTenantId()).getDatabase(databaseName);
        }
        return method.invoke(clientForTenant(tenantContext.requiredTenantId()), arguments);
    }
}
