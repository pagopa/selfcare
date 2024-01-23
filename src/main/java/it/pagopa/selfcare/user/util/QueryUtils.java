package it.pagopa.selfcare.user.util;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import it.pagopa.selfcare.user.constant.SortEnum;
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
    public Document buildQueryDocument(Map<String, Object> parameters) {
        if (!parameters.isEmpty()) {
            return bsonToDocument(Filters.and(constructBsonFilter(parameters)));
        } else {
            return new Document();
        }
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
     * The constructBsonFilter function takes a Map of parameters and returns a List of Bson objects.
     * The function iterates over the entries in the parameter map, and for each entry it creates a Bson object.
     * If the value is an ArrayList, then it creates an &quot;in&quot; filter with that key and value.
     * Otherwise, if there are any keys containing dots (.), then it calls constructElementMatch to create an element match filter with those keys/values;
     * otherwise, it creates an &quot;eq&quot; filter with that key/value pair.
     */
    private List<Bson> constructBsonFilter(Map<String, Object> parameters) {
        return parameters.entrySet().stream()
                .map(entry -> {
                    if (entry.getValue() instanceof ArrayList<?>) {
                        return Filters.in(entry.getKey(), (Iterable<?>) entry.getValue());
                    }else if(isPresentArrayFilter(parameters)){
                        Map<String, Object> finalParameters = parameters.entrySet().stream().filter(stringObjectEntry -> stringObjectEntry.getKey().contains("\\."))
                                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (x, y) -> x));
                        return constructElementMatch(finalParameters);
                    }
                    return Filters.eq(entry.getKey(), entry.getValue());
                }).toList();
    }

    /**
     * The constructElementMatch function takes a map of parameters and returns a Bson object.
     * The function is used to construct an element match filter for MongoDB queries.
     */
    public Bson constructElementMatch(Map<String, Object> parameters) {
        String arrayToMatch = Arrays.toString(parameters.keySet().stream().map(s -> s.split("\\."))
                .toList()
                .get(0));
        return Filters.elemMatch(arrayToMatch, Filters.and(constructBsonFilter(parameters)));
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
     * The bsonToDocument function converts a Bson object to a Document.
     */
    private Document bsonToDocument(Bson bson) {
        BsonDocument bsonDocument = bson.toBsonDocument(BsonDocument.class, MongoClientSettings.getDefaultCodecRegistry());
        DocumentCodec codec = new DocumentCodec();
        DecoderContext decoderContext = DecoderContext.builder().build();
        return codec.decode(new BsonDocumentReader(bsonDocument), decoderContext);
    }


    private boolean isPresentArrayFilter(Map<String, Object> parameters) {
        return parameters.entrySet().stream().anyMatch(stringObjectEntry -> stringObjectEntry.getKey().contains("\\."));
    }
}
