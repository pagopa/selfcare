package it.pagopa.selfcare.iam.entity;

import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntityBase;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.iam.model.ProductRoles;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.bson.codecs.pojo.annotations.BsonId;

import java.util.List;
import java.util.Optional;

/**
 * Entity representing user claims and permissions.
 * 
 * <h2>MongoDB Collection</h2>
 * <pre>userClaims</pre>
 * 
 * <h2>Document Structure</h2>
 * <pre>
 * {
 *   "_id": "user@example.com",
 *   "uid": "uuid",
 *   "productRoles": [
 *     {
 *       "productId": "product1",
 *       "roles": ["admin"]
 *     }
 *   ]
 * }
 * </pre>
 * 
 * <h2>Relationships</h2>
 * <ul>
 *   <li>Contains embedded {@link ProductRoles} (1:N)</li>
 *   <li>References {@link Roles} via productRoles.roles array</li>
 * </ul>
 * 
 * <h2>Indexes</h2>
 * <ul>
 *   <li>Primary: _id (email)</li>
 *   <li>Secondary: uid (unique)</li>
 * </ul>
 * 
 * @see ProductRoles
 * @see Roles
 * @see UserPermissionsRepository#getUserPermissions
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@FieldNameConstants(asEnum = true)
@MongoEntity(collection = "userClaims")
public class UserClaims extends ReactivePanacheMongoEntityBase {

  @BsonId
  private String uid;
  private String email;
  private String name;
  private String familyName;
  @Builder.Default
  private List<ProductRoles> productRoles = List.of();
  private boolean test;


  public static Uni<UserClaims> findByUid(String uid) {
    return find("_id", uid).firstResult().map(entity -> (UserClaims) entity);
  }

  public static Uni<UserClaims> findByEmail(String email) {
    return find("email", email).firstResult().map(entity -> (UserClaims) entity);
  }

  public static Uni<UserClaims> findByUidAndProductId(String uid, String productId) {
    return Optional.ofNullable(productId)
            .map(pid ->
                    find("_id = ?1 and productRoles.productId = ?2", uid, pid)
                            .firstResult()
                            .map(entity -> (UserClaims) entity)
                            .onItem().ifNull().switchTo(() ->
                                    find("_id = ?1 and productRoles.productId = ?2", uid, "ALL")
                                            .firstResult()
                                            .map(entity -> (UserClaims) entity)
                            )
            )
            .orElseGet(() -> findByUid(uid));
  }

}
