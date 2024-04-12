package it.pagopa.selfcare.user.event.entity;

import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.bson.types.ObjectId;

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

}
