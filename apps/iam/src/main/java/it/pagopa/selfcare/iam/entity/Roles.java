package it.pagopa.selfcare.iam.entity;

import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntityBase;
import it.pagopa.selfcare.iam.model.ProductRoles;
import java.util.List;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.bson.codecs.pojo.annotations.BsonId;

/**
 * Entity representing roles and their associated permissions.
 *
 * <h2>MongoDB Collection</h2>
 *
 * <pre>roles</pre>
 *
 * <h2>Document Structure</h2>
 *
 * <pre>
 * {
 *   "_id": "admin",
 *   "permissions": [
 *     "read",
 *     "write",
 *     "delete"
 *   ]
 * }
 * </pre>
 *
 * <h2>Relationships</h2>
 *
 * <ul>
 *   <li>Referenced by {@link it.pagopa.selfcare.iam.entity.UserClaims} via productRoles.roles array
 * </ul>
 *
 * <h2>Indexes</h2>
 *
 * <ul>
 *   <li>Primary: _id (role name)
 * </ul>
 *
 * @see ProductRoles
 * @see it.pagopa.selfcare.iam.entity.UserClaims
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@FieldNameConstants(asEnum = true)
@MongoEntity(collection = "roles")
public class Roles extends ReactivePanacheMongoEntityBase {

  @BsonId private String name;
  private List<String> permissions;
}
