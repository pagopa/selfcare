package it.pagopa.selfcare.onboarding.utils;

import static it.pagopa.selfcare.onboarding.utils.QueryUtils.FieldNames.*;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.model.Filters;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import it.pagopa.selfcare.onboarding.common.WorkflowType;
import org.bson.BsonDocument;
import org.bson.BsonDocumentReader;
import org.bson.Document;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.DocumentCodec;
import org.bson.conversions.Bson;

public class QueryUtils {

    private QueryUtils() {
    }

    static class FieldNames {
        private FieldNames() {
        }

        public static final String STATUS = "status";
        public static final String PRODUCT = "productId";
        public static final String WORKFLOW_TYPE = "workflowType";
        public static final String INSTITUTION_TAX_CODE = "institution.taxCode";
        public static final String INSTITUTION_ID = "institution.id";
        public static final String INSTITUTION_ORIGIN = "institution.origin";
        public static final String INSTITUTION_ORIGIN_ID = "institution.originId";
        public static final String INSTITUTION_SUBUNIT_CODE = "institution.subunitCode";
    }

    public static Document buildQuery(Map<String, String> parameters) {
        if (!parameters.isEmpty()) {
            return bsonToDocument(Filters.and(constructBsonFilter(parameters)));
        } else {
            return new Document();
        }
    }

    /**
     * The constructBsonFilter function takes a Map of parameters and returns a List of Bson objects.
     * The function iterates over the entries in the parameter map, and for each entry it creates
     * either an equality filter or a range filter depending on whether the key is &quot;from&quot; or &quot;to&quot;.
     */
    private static List<Bson> constructBsonFilter(Map<String, String> parameters) {
        return parameters.entrySet().stream()
                .map(entry -> Filters.eq(entry.getKey(), entry.getValue())).toList();
    }

    private static Document bsonToDocument(Bson bson) {
        BsonDocument bsonDocument = bson.toBsonDocument(BsonDocument.class, MongoClientSettings.getDefaultCodecRegistry());
        DocumentCodec codec = new DocumentCodec();
        DecoderContext decoderContext = DecoderContext.builder().build();
        return codec.decode(new BsonDocumentReader(bsonDocument), decoderContext);
    }


    public static Map<String, String> createMapForInstitutionOnboardingsQueryParameter(String taxCode, String subunitCode, String origin, String originId, OnboardingStatus status, String productId) {
        Map<String, String> queryParameterMap = new HashMap<>();
        getOptionalForNullableAndEmpty(taxCode).ifPresent(value -> queryParameterMap.put(INSTITUTION_TAX_CODE, value));
        queryParameterMap.put(INSTITUTION_SUBUNIT_CODE, subunitCode);
        getOptionalForNullableAndEmpty(origin).ifPresent(value -> queryParameterMap.put(INSTITUTION_ORIGIN, value));
        getOptionalForNullableAndEmpty(originId).ifPresent(value -> queryParameterMap.put(INSTITUTION_ORIGIN_ID, value));
        Optional.ofNullable(status).ifPresent(value -> queryParameterMap.put(STATUS, value.name()));
        getOptionalForNullableAndEmpty(productId).ifPresent(value -> queryParameterMap.put(PRODUCT, value));
        return queryParameterMap;
    }

    public static Map<String, String> createMapForOnboardingUsers(String institutionId, String productId) {
        Map<String, String> queryParameterMap = new HashMap<>();
        queryParameterMap.put(INSTITUTION_ID, institutionId);
        queryParameterMap.put(PRODUCT, productId);
        queryParameterMap.put(WORKFLOW_TYPE, WorkflowType.USERS.name());
        queryParameterMap.put(STATUS, OnboardingStatus.COMPLETED.name());
        return queryParameterMap;
    }

    /**
     * The getOptionalForNullableAndEmpty function takes a string value and returns an Optional object.
     * The optional object will have a value if the input string has a value and is not blank.
     * Otherwise, the optional object will be empty.
     */
    private static Optional<String> getOptionalForNullableAndEmpty(String value) {
        return Optional.ofNullable(value).filter(v -> !v.isBlank());
    }


}
