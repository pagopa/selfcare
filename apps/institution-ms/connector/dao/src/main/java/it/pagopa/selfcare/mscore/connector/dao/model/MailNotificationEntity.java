package it.pagopa.selfcare.mscore.connector.dao.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Sharded;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Set;

@Data
@NoArgsConstructor
@Document("MailNotification")
@Sharded(shardKey = {"id"})
public class MailNotificationEntity {

    public static final String FIELD_ID = "id";
    public static final String FIELD_INSTITUTION_ID = "institutionId";
    public static final String FIELD_MODULE_DAY_OF_THE_EPOCH = "moduleDayOfTheEpoch";
    public static final String FIELD_PRODUCT_IDS = "productIds";
    public static final String FIELD_DIGITAL_ADDRESS = "digitalAddress";
    public static final String FIELD_CREATED_AT = "createdAt";
    public static final String FIELD_UPDATED_AT = "updatedAt";

    @BsonId
    private ObjectId id;

    @NotNull
    @Indexed(unique = true)
    private String institutionId;

    @NotNull
    private Set<String> productIds;

    @NotNull
    @Indexed
    private Integer moduleDayOfTheEpoch;

    @NotNull
    @Email
    private String digitalAddress;

    private Instant createdAt;
    private Instant updatedAt;

}
