package it.pagopa.selfcare.user.entity;

import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntity;
import it.pagopa.selfcare.user.model.OnboardedProduct;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.bson.types.ObjectId;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@MongoEntity(collection = "userInstitutions")
@FieldNameConstants(asEnum = true)
public class UserInstitution extends ReactivePanacheMongoEntity {

    private ObjectId id;
    private String userId;
    private String institutionId;
    private String institutionDescription;
    private String institutionRootName;
    private List<OnboardedProduct> products = new ArrayList<>();
    private String userMailUuid;
    private OffsetDateTime userMailUpdatedAt;

    /**
     * Tenant that owns this record (Step_0 sub-task 6). Null on documents written before the
     * discriminator existed; those stay visible to both tenants until the backfill has run.
     */
    private String tenantId;
}
