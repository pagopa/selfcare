package it.pagopa.selfcare.iam.entity;

import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntityBase;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.iam.model.ProductRoles;
import java.util.List;
import java.util.Optional;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.bson.codecs.pojo.annotations.BsonId;

/**
 * Entity representing user claims and permissions.
 *
 * <h2>MongoDB Collection</h2>
 *
 * <pre>userClaims</pre>
 *
 * <h2>Document Structure</h2>
 *
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
 *
 * <ul>
 *   <li>Contains embedded {@link ProductRoles} (1:N)
 *   <li>References {@link Roles} via productRoles.roles array
 * </ul>
 *
 * <h2>Indexes</h2>
 *
 * <ul>
 *   <li>Primary: _id (email)
 *   <li>Secondary: uid (unique)
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

  @BsonId private String uid;
  private String email;
  private String name;
  private String familyName;
  @Builder.Default private List<ProductRoles> productRoles = List.of();
  private boolean test;

  public static Uni<UserClaims> findByUid(String uid) {
    return find("_id", uid).firstResult().map(entity -> (UserClaims) entity);
  }

  public static Uni<UserClaims> findByEmail(String email) {
    return find("email", email).firstResult().map(entity -> (UserClaims) entity);
  }

  public static Uni<UserClaims> findByUidAndProductId(String uid, String productId) {
    return Optional.ofNullable(productId)
        .map(
            pid ->
                find("_id = ?1 and productRoles.productId = ?2", uid, pid)
                    .firstResult()
                    .map(entity -> (UserClaims) entity)
                    .onItem()
                    .ifNull()
                    .switchTo(
                        () ->
                            find("_id = ?1 and productRoles.productId = ?2", uid, "ALL")
                                .firstResult()
                                .map(entity -> (UserClaims) entity)))
        .orElseGet(() -> findByUid(uid));
  }

  public static Uni<List<UserClaims>> findByProductId(String productId) {
    return Optional.ofNullable(productId)
        .map(
            pid ->
                find("productRoles.productId = ?1", pid)
                    .list()
                    .map(list -> list.stream().map(entity -> (UserClaims) entity).toList()))
        .orElseGet(() -> Uni.createFrom().item(List.of()));
  }
}
