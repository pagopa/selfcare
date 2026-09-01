package it.pagopa.selfcare.institution.event.entity;

import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.bson.codecs.pojo.annotations.BsonProperty;

@EqualsAndHashCode(callSuper = true)
@Data
@MongoEntity(collection = "Institution")
@FieldNameConstants(asEnum = true)
public class InstitutionEntity extends ReactivePanacheMongoEntity {

    @BsonProperty("_id")
    private String id;
    private String description;

}
