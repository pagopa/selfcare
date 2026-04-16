package it.pagopa.selfcare.institution.entity;

import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bson.types.ObjectId;

import java.time.Instant;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@MongoEntity(collection="MailNotification")
public class MailNotification extends ReactivePanacheMongoEntity {

    public static final String FIELD_ID = "id";
    public static final String FIELD_INSTITUTION_ID = "institutionId";
    public static final String FIELD_MODULE_DAY_OF_THE_EPOCH = "moduleDayOfTheEpoch";
    public static final String FIELD_PRODUCT_IDS = "productIds";
    public static final String FIELD_DIGITAL_ADDRESS = "digitalAddress";
    public static final String FIELD_CREATED_AT = "createdAt";
    public static final String FIELD_UPDATED_AT = "updatedAt";

    private ObjectId id;
    private Integer moduleDayOfTheEpoch;
    private List<String> productIds;
    private String institutionId;
    private String digitalAddress;
    private Instant createdAt;
    private Instant updatedAt;

}
