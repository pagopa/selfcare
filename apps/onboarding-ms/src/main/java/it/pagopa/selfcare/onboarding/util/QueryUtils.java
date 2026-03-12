package it.pagopa.selfcare.onboarding.util;

import static it.pagopa.selfcare.onboarding.util.QueryUtils.FieldNames.*;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.model.OnboardingGetFilters;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.apache.commons.lang3.StringUtils;
import org.bson.BsonDocument;
import org.bson.BsonDocumentReader;
import org.bson.Document;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.DocumentCodec;
import org.bson.conversions.Bson;

public class QueryUtils {

  private QueryUtils() {
  }

  public static class FieldNames {
    private FieldNames() {
    }

    public static final String STATUS = "status";
    public static final String FROM = "from";
    public static final String TO = "to";
    public static final String PRODUCT = "productId";
    public static final String INSTITUTION_TAX_CODE = "institution.taxCode";
    public static final String INSTITUTION_ID = "institution.id";
    public static final String INSTITUTION_ORIGIN = "institution.origin";
    public static final String INSTITUTION_ORIGIN_ID = "institution.originId";
    public static final String INSTITUTION_SUBUNIT_CODE = "institution.subunitCode";
    public static final String USER_ID = "users._id";
  }

  public static Document buildQuery(Map<String, Object> parameters) {
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
  private static List<Bson> constructBsonFilter(Map<String, Object> parameters) {
    return parameters.entrySet().stream()
      .map(entry -> {
        if (StringUtils.equalsIgnoreCase(entry.getKey(), FROM)) {
          return Filters.gte(Onboarding.Fields.createdAt.name(), LocalDate.parse((String) entry.getValue(), DateTimeFormatter.ISO_LOCAL_DATE));
        } else if (StringUtils.equalsIgnoreCase(entry.getKey(), TO)) {
          return Filters.lt(Onboarding.Fields.createdAt.name(), LocalDate.parse((String) entry.getValue(), DateTimeFormatter.ISO_LOCAL_DATE).plusDays(1));
        } else if (entry.getValue() instanceof List<?>) {
          return Filters.in(entry.getKey(), (Iterable<?>) entry.getValue());
        }
        return Filters.eq(entry.getKey(), entry.getValue());
      }).toList();
  }

  private static Document bsonToDocument(Bson bson) {
    BsonDocument bsonDocument = bson.toBsonDocument(BsonDocument.class, MongoClientSettings.getDefaultCodecRegistry());
    DocumentCodec codec = new DocumentCodec();
    DecoderContext decoderContext = DecoderContext.builder().build();
    return codec.decode(new BsonDocumentReader(bsonDocument), decoderContext);
  }

  /**
   * The createMapForOnboardingQueryParameter function creates a map of query parameters for the Onboarding Collection.
   */
  public static Map<String, Object> createMapForOnboardingQueryParameter(OnboardingGetFilters filters) {
    Map<String, Object> queryParameterMap = new HashMap<>();
    Optional.ofNullable(filters.getUserId()).ifPresent(value -> queryParameterMap.put(USER_ID, filters.getUserId()));
    Optional.ofNullable(filters.getProductId()).ifPresent(value -> queryParameterMap.put(PRODUCT, value));
    Optional.ofNullable(filters.getTaxCode()).ifPresent(value -> queryParameterMap.put(INSTITUTION_TAX_CODE, value));
    Optional.ofNullable(filters.getSubunitCode()).ifPresent(value -> queryParameterMap.put(INSTITUTION_SUBUNIT_CODE, value));
    Optional.ofNullable(filters.getStatus()).ifPresent(value -> queryParameterMap.put(STATUS, value));
    Optional.ofNullable(filters.getFrom()).ifPresent(value -> queryParameterMap.put(FROM, value));
    Optional.ofNullable(filters.getTo()).ifPresent(value -> queryParameterMap.put(TO, value));
    Optional.ofNullable(filters.getInstitutionId()).ifPresent(value -> queryParameterMap.put(INSTITUTION_ID, value));
    Optional.ofNullable(filters.getOnboardingId()).ifPresent(value -> queryParameterMap.put("_id", value));
    if (Objects.nonNull(filters.getProductIds()) && !filters.getProductIds().isEmpty()) {
      queryParameterMap.put(PRODUCT, filters.getProductIds());
    }
    return queryParameterMap;
  }

  public static Map<String, Object> createMapForInstitutionOnboardingsQueryParameter(String taxCode, String subunitCode, String origin, String originId, OnboardingStatus status, String productId) {
    Map<String, Object> queryParameterMap = new HashMap<>();
    getOptionalForNullableAndEmpty(taxCode).ifPresent(value -> queryParameterMap.put(INSTITUTION_TAX_CODE, value));
    queryParameterMap.put(INSTITUTION_SUBUNIT_CODE, subunitCode);
    getOptionalForNullableAndEmpty(origin).ifPresent(value -> queryParameterMap.put(INSTITUTION_ORIGIN, value));
    getOptionalForNullableAndEmpty(originId).ifPresent(value -> queryParameterMap.put(INSTITUTION_ORIGIN_ID, value));
    Optional.ofNullable(status).ifPresent(value -> queryParameterMap.put(STATUS, value.name()));
    getOptionalForNullableAndEmpty(productId).ifPresent(value -> queryParameterMap.put(PRODUCT, value));
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

  public static Map<String, Object> createMapForOnboardingReject(String reasonForReject, String onboardingStatus) {
    Map<String, Object> queryParameterMap = new HashMap<>();
    Optional.ofNullable(reasonForReject).ifPresent(value -> queryParameterMap.put("reasonForReject", value));
    Optional.ofNullable(onboardingStatus).ifPresent(value -> queryParameterMap.put(STATUS, value));
    queryParameterMap.put("updatedAt", LocalDateTime.now());
    return queryParameterMap;
  }

  public static Map<String, Object> createMapForOnboardingUpdate(Onboarding onboarding) {
    Map<String, Object> queryParameterMap = new HashMap<>();
    // Dates
    Optional.ofNullable(onboarding.getActivatedAt()).ifPresent(value -> queryParameterMap.put("activatedAt", value));
    Optional.ofNullable(onboarding.getCreatedAt()).ifPresent(value -> queryParameterMap.put("createdAt", value));
    Optional.ofNullable(onboarding.getDeletedAt()).ifPresent(value -> queryParameterMap.put("deletedAt", value));
    // Root elements
    Optional.ofNullable(onboarding.getStatus()).ifPresent(value -> queryParameterMap.put(STATUS, value.name()));
    // Billing
    Optional.ofNullable(onboarding.getBilling())
      .ifPresent(billing -> {
        Optional.ofNullable(billing.getRecipientCode()).ifPresent(value -> queryParameterMap.put("billing.recipientCode", value));
        Optional.ofNullable(billing.getVatNumber()).ifPresent(value -> queryParameterMap.put("billing.vatNumber", value));
        Optional.ofNullable(billing.getTaxCodeInvoicing()).ifPresent(value -> queryParameterMap.put("billing.taxCodeInvoicing()", value));
      });
    // Institution
    Optional.ofNullable(onboarding.getInstitution())
      .ifPresent(institution -> {
        Optional.ofNullable(institution.getAddress()).ifPresent(value -> queryParameterMap.put("institution.address", value));
        Optional.ofNullable(institution.getDescription()).ifPresent(value -> queryParameterMap.put("institution.description", value));
        Optional.ofNullable(institution.getDigitalAddress()).ifPresent(value -> queryParameterMap.put("institution.digitalAddress", value));
        Optional.ofNullable(institution.getCity()).ifPresent(value -> queryParameterMap.put("institution.city", value));
        Optional.ofNullable(institution.getCountry()).ifPresent(value -> queryParameterMap.put("institution.country", value));
        Optional.ofNullable(institution.getCounty()).ifPresent(value -> queryParameterMap.put("institution.county", value));
        Optional.ofNullable(institution.getZipCode()).ifPresent(value -> queryParameterMap.put("institution.zipCode", value));
        Optional.ofNullable(institution.getIstatCode()).ifPresent(value -> queryParameterMap.put("institution.istatCode", value));
        Optional.ofNullable(institution.getRea()).ifPresent(value -> queryParameterMap.put("institution.rea", value));
        Optional.ofNullable(institution.getShareCapital()).ifPresent(value -> queryParameterMap.put("institution.shareCapital", value));
        Optional.ofNullable(institution.getBusinessRegisterPlace()).ifPresent(value -> queryParameterMap.put("institution.businessRegisterPlace", value));
        Optional.ofNullable(institution.getSupportEmail()).ifPresent(value -> queryParameterMap.put("institution.supportEmail", value));
        Optional.ofNullable(institution.getParentDescription()).ifPresent(value -> queryParameterMap.put("institution.parentDescription", value));
        Optional.ofNullable(institution.getOrigin()).ifPresent(value -> queryParameterMap.put(INSTITUTION_ORIGIN, value.name()));
        Optional.ofNullable(institution.getOriginId()).ifPresent(value -> queryParameterMap.put(INSTITUTION_ORIGIN_ID, value));
      });
    queryParameterMap.put("updatedAt", LocalDateTime.now());
    return queryParameterMap;
  }

  public static Document buildSortDocument(String field, SortEnum order) {
    if (SortEnum.ASC == order) {
      return bsonToDocument(Sorts.ascending(field));
    } else {
      return bsonToDocument(Sorts.descending(field));
    }
  }

  public static Document buildUpdateDocument(Map<String, Object> parameters) {
    if (!parameters.isEmpty()) {
      return bsonToDocument(Updates.combine(constructBsonUpdate(parameters)));
    } else {
      return new Document();
    }
  }

  private static List<Bson> constructBsonUpdate(Map<String, Object> parameters) {
    return parameters.entrySet()
      .stream()
      .map(stringStringEntry -> Updates.set(stringStringEntry.getKey(), stringStringEntry.getValue()))
      .toList();
  }

}
