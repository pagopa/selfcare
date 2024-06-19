package it.pagopa.selfcare.user.util;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import it.pagopa.selfcare.user.constant.SortEnum;
import it.pagopa.selfcare.user.entity.UserInstitutionRole;
import it.pagopa.selfcare.user.entity.filter.OnboardedProductFilter;
import it.pagopa.selfcare.user.entity.filter.UserInstitutionRoleFilter;
import it.pagopa.selfcare.user.model.OnboardedProduct;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.bson.BsonDocument;
import org.bson.BsonDocumentReader;
import org.bson.Document;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.DocumentCodec;
import org.bson.conversions.Bson;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static it.pagopa.selfcare.user.constant.CollectionUtil.USER_INSTITUTION_COLLECTION;
import static java.util.stream.Collectors.groupingBy;

@RequiredArgsConstructor(access = AccessLevel.NONE)
@ApplicationScoped
public class QueryUtils {

    /**
     * The buildUpdateDocument function takes a map of parameters and constructs an update document
     * that can be used to update the database. The function uses the Updates class from MongoDB's
     * Java driver to construct a Bson object, which is then converted into a Document object. If no
     * parameters are passed in, an empty Document is returned instead. This allows for flexibility in
     * how this function can be called by other functions within this class (e.g., if you want to pass
     * in only one parameter).
     */
    public Document buildUpdateDocument(Map<String, Object> parameters) {
        if (!parameters.isEmpty()) {
            return bsonToDocument(Updates.combine(constructBsonUpdate(parameters)));
        } else {
            return new Document();
        }
    }

    /**
     * The buildQueryDocument function takes a map of parameters and constructs a MongoDB query document.
     * The function iterates through the key-value pairs in the map, and for each pair it adds an entry to
     * the query document. If there are multiple entries in the map, then they are combined using logical ANDs.
     */
    public Document buildQueryDocument(Map<String, Object> parameters, String collection) {
        if (!parameters.isEmpty()) {
            return bsonToDocument(Filters.and(constructBsonFilter(parameters, collection)));
        } else {
            return new Document();
        }
    }
    public Document buildQueryDocumentByDate(Map<String, Object> parameters, String collection, LocalDateTime fromDate) {
            return bsonToDocument(Filters.and(constructBsonWithDateFilter(parameters, collection, fromDate)));

    }

    /**
     * The buildSortDocument function takes a field and an order as parameters.
     * It then returns a Document object that can be used to sort the results of a query.
     */
    public Document buildSortDocument(String field, SortEnum order) {
        if (SortEnum.ASC == order) {
            return bsonToDocument(Sorts.ascending(field));
        } else {
            return bsonToDocument(Sorts.descending(field));
        }
    }

    /**
     * The bsonToDocument function converts a Bson object to a Document.
     */
    private Document bsonToDocument(Bson bson) {
        BsonDocument bsonDocument = bson.toBsonDocument(BsonDocument.class, MongoClientSettings.getDefaultCodecRegistry());
        DocumentCodec codec = new DocumentCodec();
        DecoderContext decoderContext = DecoderContext.builder().build();
        return codec.decode(new BsonDocumentReader(bsonDocument), decoderContext);
    }

    /**
     * The constructBsonUpdate function takes a Map of String keys to Object values and returns a List of Bson objects.
     * The function uses the Updates class from the MongoDB Java driver to create an update operation for each key-value pair in the map.
     */
    private List<Bson> constructBsonUpdate(Map<String, Object> parameters) {
        return parameters.entrySet()
                .stream()
                .map(stringStringEntry -> Updates.set(stringStringEntry.getKey(), stringStringEntry.getValue()))
                .toList();
    }



    /**
     * The constructBsonFilter function takes in a map of parameters and the name of the collection
     * that we are querying.
     * The function first checks if there is an array field present in the parameter map, and if so it creates
     * a new Map object containing only those fields that need to be filtered by elemMatch (i.e., arrays).
     * This new Map object is then passed into addEleMatchOperatorAndCleanParameterMap(),
     * which adds an $elemMatch operator to each key-value pair in this new Map object and removes these
     * Finally iterates on cleaned parameters map to add in or eq query operator and
     * returns a list of Bson objects, which will be used to filter our query.
     */
    private List<Bson> constructBsonFilter(Map<String, Object> parameters, String collection) {
        List<Bson> bsonList = new ArrayList<>();

        Map<String, Object> mapForElemMatch = retrieveArrayFilterIfPresent(parameters, collection);

        if (!mapForElemMatch.isEmpty()) {
            addElemMatchOperator(mapForElemMatch, collection, bsonList);
            parameters = parameters.entrySet().stream().filter(stringObjectEntry -> !mapForElemMatch.containsKey(stringObjectEntry.getKey()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }

        bsonList.addAll(addEqAndInFilters(parameters));
        return bsonList;
    }

    private List<Bson> constructBsonWithDateFilter(Map<String, Object> parameters, String collection, LocalDateTime fromDate){
        List<Bson> bsonList = new ArrayList<>();
        if (!parameters.isEmpty()) {
            Map<String, Object> mapForElemMatch = retrieveArrayFilterIfPresent(parameters, collection);

            if (!mapForElemMatch.isEmpty()) {
                addElemMatchOperator(mapForElemMatch, collection, bsonList);
                parameters = parameters.entrySet().stream()
                        .filter(entry -> !mapForElemMatch.containsKey(entry.getKey()))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            }

            bsonList.addAll(addEqAndInFilters(parameters));
        }
        if (fromDate != null) {
            bsonList.add(Filters.elemMatch("products", Filters.gt("createdAt", fromDate)));
        }

        return bsonList;
    }
    /**
     * The addElemMatchOperator function is used to add the $elemMatch operator to the query.
     * The function takes in a mapForElemMatch map, parameters map, collection name and bsonList as arguments.
     * It then iterates through all of the keys in mapForElemMatch and retrieves their parent key using retrieveParent() from enum
     * chosen based on collection argument.
     * Then it groups all of these parent keys together into a Map&lt;String, List&lt;String&gt;&gt; object called groupedParentsMap.
     * Finally it iterates through each entry in groupedParentsMap and adds an elemMatch filter for each one with its value being
     * an AND filter containing all of the eq and in filters from addAnd
     */
    private void addElemMatchOperator(Map<String, Object> mapForElemMatch, String collection, List<Bson> bsonList) {
        mapForElemMatch.keySet()
                .stream().map(key -> retrieveParent(key, collection))
                .collect(groupingBy(o -> o))
                .forEach((s, strings) -> bsonList.add(Filters.elemMatch(s, Filters.and(addEqAndInFilters(mapForElemMatch)))));
    }

    /**
     * The addEqAndInFilters function takes a Map of parameters and returns a List of Bson objects.
     * The function iterates over the entries in the parameter map, and for each entry it creates
     * either an eq or an in filter depending on whether the value is an ArrayList or not. It then adds
     * that filter to a list which it returns at the end of its execution. This function is used by both
     * findDocumentsWithParameters and updateDocumentsWithParameters to create filters based on user input.
     */
    private List<Bson> addEqAndInFilters(Map<String, Object> parameters) {
        return parameters.entrySet().stream()
                .map(entry -> {
                    if (entry.getValue() instanceof List<?>) {
                        return Filters.in(entry.getKey(), (Iterable<?>) entry.getValue());
                    }
                    return Filters.eq(entry.getKey(), entry.getValue());
                }).toList();
    }


    /**
     * The retrieveParent function is used to retrieve the parent of a given key.
     * The parent is retrieved from enum related to given collection name;
     */
    private String retrieveParent(String key, String collection) {
        if (USER_INSTITUTION_COLLECTION.equalsIgnoreCase(collection)) {
            return OnboardedProductFilter.OnboardedProductEnum.retrieveParent(key)
                    .orElse(null);
        } else {
            return UserInstitutionRoleFilter.UserInstitutionRoleEnum.retrieveParent(key)
                    .orElse(null);
        }
    }


    /**
     * The retrieveArrayFilterIfPresent function is used to filter out the parameters that are not part of the
     * OnboardedProduct.Fields or UserInstitutionRole.Fields enumerations, depending on which collection is being queried.
     * This function retrieve a new parameterMap ,containing only those filters necessary to build an element match filter
     * The second argument represents which collection we are filtering for (either USER_INSTITUTION_COLLECTION or USER_INFO_COLLECTION)
     */
    private Map<String, Object> retrieveArrayFilterIfPresent(Map<String, Object> parameters, String collection) {
        if (USER_INSTITUTION_COLLECTION.equalsIgnoreCase(collection)) {
            return parameters.entrySet().stream()
                    .filter(mapEntry -> Arrays.stream(OnboardedProduct.Fields.values()).anyMatch(field -> field.name().equalsIgnoreCase(mapEntry.getKey())))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (x, y) -> x));
        } else {
            return parameters.entrySet().stream()
                    .filter(mapEntry -> Arrays.stream(UserInstitutionRole.Fields.values()).anyMatch(field -> field.name().equalsIgnoreCase(mapEntry.getKey())))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (x, y) -> x));
        }
    }
}
