package it.pagopa.selfcare.document.entity;

import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntityBase;
import it.pagopa.selfcare.onboarding.common.TokenType;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bson.codecs.pojo.annotations.BsonId;

@EqualsAndHashCode(callSuper = true)
@Data
@MongoEntity(collection="documents")
public class Document extends ReactivePanacheMongoEntityBase {

    @BsonId
    private String id;
    private TokenType type;
    private String onboardingId;
    private String productId;
    private String name;
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

