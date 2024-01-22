package it.pagopa.selfcare.user.util;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import it.pagopa.selfcare.user.constant.SortEnum;
import it.pagopa.selfcare.user.entity.UserInstitution;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.bson.BsonDocument;
import org.bson.BsonDocumentReader;
import org.bson.Document;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.DocumentCodec;
import org.bson.conversions.Bson;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor(access = AccessLevel.NONE)
@ApplicationScoped
public class QueryUtils {

    private static final String CURRENT_PRODUCT = "products.";

    public Document buildUpdateDocument(Map<String, Object> parameters) {
        if (!parameters.isEmpty()) {
            return bsonToDocument(Updates.combine(constructBsonUpdate(parameters)));
        } else {
            return new Document();
        }
    }

    public Document buildQueryDocument(Map<String, Object> parameters) {
        if (!parameters.isEmpty()) {
            return bsonToDocument(Filters.and(constructBsonFilter(parameters)));
        } else {
            return new Document();
        }
    }

    public Document buildSortDocument(String field, SortEnum order) {
        if (SortEnum.ASC == order) {
            return bsonToDocument(Sorts.ascending(field));
        } else {
            return bsonToDocument(Sorts.descending(field));
        }
    }

    public Bson constructElementMatch(Map<String, Object> parameters) {
        String arrayToMatch = Arrays.toString(parameters.keySet().stream().map(s -> s.split("\\."))
                .toList()
                .get(0));
        return Filters.elemMatch(arrayToMatch, Filters.and(constructBsonFilter(parameters)));
    }

    /**
     * The constructBsonFilter function takes a Map of parameters and returns a List of Bson objects.
     * The function iterates over the entries in the parameter map, and for each entry it creates
     * either an equality filter or a range filter depending on whether the key is &quot;from&quot; or &quot;to&quot;.
     */
    private List<Bson> constructBsonFilter(Map<String, Object> parameters) {
        return parameters.entrySet().stream()
                .map(entry -> {
                    if (entry.getValue() instanceof ArrayList<?>) {
                        return Filters.in(entry.getKey(), entry.getValue());
                    }else if(isPresentArrayFilter(parameters)){
                        Map<String, Object> finalParameters = parameters.entrySet().stream().filter(stringObjectEntry -> stringObjectEntry.getKey().contains("\\."))
                                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (x, y) -> x));
                        return constructElementMatch(finalParameters);
                    }
                    return Filters.eq(entry.getKey(), entry.getValue());
                }).toList();
    }

    private List<Bson> constructBsonUpdate(Map<String, Object> parameters) {
        return parameters.entrySet()
                .stream()
                .map(stringStringEntry -> Updates.set(stringStringEntry.getKey(), stringStringEntry.getValue()))
                .toList();
    }

    private Document bsonToDocument(Bson bson) {
        BsonDocument bsonDocument = bson.toBsonDocument(BsonDocument.class, MongoClientSettings.getDefaultCodecRegistry());
        DocumentCodec codec = new DocumentCodec();
        DecoderContext decoderContext = DecoderContext.builder().build();
        return codec.decode(new BsonDocumentReader(bsonDocument), decoderContext);
    }

    /**
     * The createMapForUserQueryParameter function creates a map of query parameters for the User Collection.
     */
    public Map<String, Object> createMapForUserQueryParameter(String userId, String institutionId, String productId, List<String> status, String role, String productRole) {
        Map<String, Object> queryParameterMap = new HashMap<>();
        Optional.ofNullable(userId).ifPresent(value -> queryParameterMap.put(UserInstitution.Fields.userId.name(), value));
        Optional.ofNullable(institutionId).ifPresent(value -> queryParameterMap.put(UserInstitution.Fields.institutionId.name(), value));
        Optional.ofNullable(productId).ifPresent(value -> queryParameterMap.put(UserInstitution.Fields.products.name() + ".productId", value));
        Optional.ofNullable(status).ifPresent(value -> queryParameterMap.put(UserInstitution.Fields.products.name() + ".status", value));
        Optional.ofNullable(role).ifPresent(value -> queryParameterMap.put(UserInstitution.Fields.products.name() + ".role", value));
        Optional.ofNullable(productRole).ifPresent(value -> queryParameterMap.put(UserInstitution.Fields.products.name() + ".productRole", value));
        return queryParameterMap;
    }

    public Map<String, Object> createMapForUserUpdateParameter(String userId, String institutionId, String productId, List<String> status, String role, String productRole, String relationshipId) {
        Map<String, Object> queryParameterMap = new HashMap<>();
        Optional.ofNullable(userId).ifPresent(value -> queryParameterMap.put(UserInstitution.Fields.userId.name(), value));
        Optional.ofNullable(institutionId).ifPresent(value -> queryParameterMap.put(UserInstitution.Fields.institutionId.name(), value));
        Optional.ofNullable(productId).ifPresent(value -> queryParameterMap.put(CURRENT_PRODUCT + "productId", value));
        Optional.ofNullable(status).ifPresent(value -> queryParameterMap.put(CURRENT_PRODUCT + "status", value));
        Optional.ofNullable(role).ifPresent(value -> queryParameterMap.put(CURRENT_PRODUCT + "role", value));
        Optional.ofNullable(productRole).ifPresent(value -> queryParameterMap.put(CURRENT_PRODUCT + "productRole", value));
        Optional.ofNullable(productRole).ifPresent(value -> queryParameterMap.put(CURRENT_PRODUCT + "relationshipId", value));
        return queryParameterMap;
    }

    private boolean isPresentArrayFilter(Map<String, Object> parameters) {
        return parameters.entrySet().stream().anyMatch(stringObjectEntry -> stringObjectEntry.getKey().contains("\\."));
    }
}
