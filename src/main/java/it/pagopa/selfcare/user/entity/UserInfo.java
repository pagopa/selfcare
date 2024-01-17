package it.pagopa.selfcare.user.entity;

import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bson.types.ObjectId;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@MongoEntity(collection = "userInfo")
public class UserInfo extends ReactivePanacheMongoEntity {

    private ObjectId id;
    private String userId;
    private List<UserInstitutionRole> institutions;

}
