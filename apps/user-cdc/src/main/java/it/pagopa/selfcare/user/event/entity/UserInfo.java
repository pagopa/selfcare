package it.pagopa.selfcare.user.event.entity;

import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntity;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntityBase;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonProperty;

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@MongoEntity(collection = "userInfo")
@FieldNameConstants(asEnum = true)
public class UserInfo extends ReactivePanacheMongoEntityBase {

    @BsonId
    @BsonProperty("_id")
    private String userId;

    private List<UserInstitutionRole> institutions = new ArrayList<>();

}
