package it.pagopa.selfcare.onboarding.event.entity;

import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntityBase;
import it.pagopa.selfcare.onboarding.common.TokenType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bson.codecs.pojo.annotations.BsonId;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@MongoEntity(collection="tokens")
public class Token extends ReactivePanacheMongoEntityBase {

    @BsonId
    private String id;
    private TokenType type;
    private String onboardingId;
    private String productId;
    private String checksum;
    private String contractVersion;
    private String contractTemplate;
    private String contractSigned;
    private String contractFilename;
    //@Indexed
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private LocalDateTime activatedAt;

}

