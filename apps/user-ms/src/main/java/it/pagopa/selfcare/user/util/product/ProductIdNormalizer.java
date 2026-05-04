package it.pagopa.selfcare.user.util.product;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import io.quarkus.scheduler.Scheduled;
import it.pagopa.selfcare.product.service.ProductService;
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

    // productId -> parentId
    private static final Map<String, String> PARENT_IDS = new ConcurrentHashMap<>();

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
        return PARENT_IDS.getOrDefault(productId, productId);
    }

    public static void addParentId(String productId, String parentId) {
        PARENT_IDS.put(productId, parentId);
    }

    public static void clearParentIds() {
        PARENT_IDS.clear();
    }

    @Scheduled(every = "10m")
    public static void updateParentIds() {
        final ProductService productService = CDI.current().select(ProductService.class).get();
        try {
            productService.getProducts(false, true).stream()
                    .filter(p -> p.getParentId() != null && !p.getParentId().isBlank())
                    .forEach(p -> PARENT_IDS.put(p.getId(), p.getParentId()));
        } catch (Exception ex) {
            log.error("Failed to update parent IDs", ex);
        }
    }

}