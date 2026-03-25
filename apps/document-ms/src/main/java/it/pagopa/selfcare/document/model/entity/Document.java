package it.pagopa.selfcare.document.model.entity;

import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntityBase;
import it.pagopa.selfcare.onboarding.common.TokenType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bson.codecs.pojo.annotations.BsonId;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@MongoEntity(collection="documents")
public class Document extends ReactivePanacheMongoEntityBase {

    @BsonId
    @Schema(description = "Chiave surrogata (Surrogate Key) generata come UUID randomico. Sostituisce l'onboardingId come Primary Key per prevenire anomalie.")
    private String id;
    @Schema(description = "Discriminante polimorfica che indica la natura del documento (INSTITUTION, USER, ATTACHMENT).")
    private TokenType type;
    @Schema(description = "ID del processo di onboarding specifico. Per il tipo ATTACHMENT, non avendo un flusso dedicato, coincide con il rootOnboardingId.")
    private String onboardingId;
    private String productId;
    private String attachmentName;
    private String checksum;
    private String contractVersion;
    private String contractTemplate;
    private String contractSigned;
    private String contractFilename;
    @Schema(description = "Identificativo della radice del Fascicolo Documentale. Corrisponde all'onboardingId dell'INSTITUTION.")
    private String rootOnboardingId;
    //@Indexed
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private LocalDateTime activatedAt;

}

