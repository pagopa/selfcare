package it.pagopa.selfcare.user.entity;

import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntityBase;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.bson.codecs.pojo.annotations.BsonId;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@MongoEntity(collection = "userInfo")
@FieldNameConstants(asEnum = true)
public class UserInfo extends ReactivePanacheMongoEntityBase {

    @BsonId
    private String userId;
    private List<UserInstitutionRole> institutions;

    /**
     * Tenant that owns this record (Step_0 sub-task 6). Null on documents written before the
     * discriminator existed; those stay visible to both tenants until the backfill has run.
     */
    private String tenantId;

}
