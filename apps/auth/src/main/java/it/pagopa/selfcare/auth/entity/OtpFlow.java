package it.pagopa.selfcare.auth.entity;

import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntity;
import it.pagopa.selfcare.auth.model.OtpStatus;
import lombok.*;
import lombok.experimental.FieldNameConstants;

import java.time.OffsetDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@MongoEntity(collection = "otpFlows")
@FieldNameConstants(asEnum = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpFlow extends ReactivePanacheMongoEntity {

    private String uuid;
    private String userId;
    private String otp;
    private OtpStatus status;
    private Integer attempts;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime expiresAt;

}
