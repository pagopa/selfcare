package it.pagopa.selfcare.user.util.product;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import io.quarkus.scheduler.Scheduled;
import it.pagopa.selfcare.tenant.TenantContext;
import it.pagopa.selfcare.tenant.TenantRegistry;
import it.pagopa.selfcare.user.service.TenantProductCatalogService;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Provider
@Slf4j
public class ProductIdNormalizer extends JsonDeserializer<String> implements ParamConverterProvider {

    private static final Map<String, Map<String, String>> PARENT_IDS = new ConcurrentHashMap<>();

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        final String value = p.getValueAsString();
        return normalize(value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> ParamConverter<T> getConverter(Class<T> aClass, Type type, Annotation[] annotations) {
        if (aClass.equals(String.class)) {
            if (Arrays.stream(annotations).anyMatch(a -> a.annotationType().equals(ProductId.class))) {
                return (ParamConverter<T>) new ParamConverter<String>() {
                    @Override
                    public String fromString(String s) {
                        return normalize(s);
                    }

                    @Override
                    public String toString(String s) {
                        return s;
                    }
                };
            }
        }
        return null;
    }

    public static String normalize(String productId) {
        if (productId == null || productId.isBlank()) {
            return productId;
        }
        String tenant = CDI.current().select(TenantContext.class).get().requiredTenantId();
        return PARENT_IDS.getOrDefault(tenant, Map.of()).getOrDefault(productId, productId);
    }

    public static void addParentId(String productId, String parentId) {
        String tenant = CDI.current().select(TenantContext.class).get().requiredTenantId();
        PARENT_IDS.computeIfAbsent(tenant, ignored -> new ConcurrentHashMap<>()).put(productId, parentId);
    }

    public static void clearParentIds() {
        PARENT_IDS.clear();
    }

    @Scheduled(every = "10m")
    public static void updateParentIds() {
        var catalog = CDI.current().select(TenantProductCatalogService.class).get();
        for (String tenant : CDI.current().select(TenantRegistry.class).get().supportedTenantIds()) {
            try {
                Map<String, String> parents = new ConcurrentHashMap<>();
                catalog.productsForTenant(tenant).stream()
                        .filter(p -> p.getParentId() != null && !p.getParentId().isBlank())
                        .forEach(p -> parents.put(p.getId(), p.getParentId()));
                PARENT_IDS.put(tenant, parents);
            } catch (RuntimeException ex) {
                log.error("Failed to update parent IDs for tenant {}", tenant, ex);
            }
        }
    }

}