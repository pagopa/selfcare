package it.pagopa.selfcare.auth.entity;

import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntity;
import it.pagopa.selfcare.auth.model.error.OtpStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.bson.codecs.pojo.annotations.BsonId;

import java.time.OffsetDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@MongoEntity(collection = "otpFlows")
@FieldNameConstants(asEnum = true)
public class OtpFlow extends ReactivePanacheMongoEntity {

    @BsonId
    private String uuid;

    private String userId;
    private String otp;
    private OtpStatus status = OtpStatus.PENDING;
    private Integer attempts = 0;
    private OffsetDateTime createdAt = OffsetDateTime.now();
    private OffsetDateTime updatedAt;
    private OffsetDateTime expiresAt;

}
