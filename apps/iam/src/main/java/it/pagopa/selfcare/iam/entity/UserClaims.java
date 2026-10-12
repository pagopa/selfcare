package it.pagopa.selfcare.iam.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.quarkus.mongodb.panache.common.MongoEntity;
import it.pagopa.selfcare.iam.model.ProductRoles;
import java.util.List;
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
 *   "_id": "uuid",
 *   "tenantId": "AR",
 *   "email": "encrypted email",
 *   "productRoles": [
 *     {
 *       "productId": "product1",
 *       "tenantId": "AR",
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
 *   <li>Primary: _id (uid)
 *   <li>Secondary: (tenantId, email)
 * </ul>
 *
 * <h2>Tenant</h2>
 *
 * <p>Each document belongs to one tenant (top-level {@code tenantId}); the same email can exist
 * once per tenant. Access goes through {@link
 * it.pagopa.selfcare.iam.repository.UserClaimsRepository}, which applies the tenant filter.
 *
 * @see ProductRoles
 * @see Roles
 * @see it.pagopa.selfcare.iam.repository.UserPermissionsRepository#getUserPermissions
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@FieldNameConstants(asEnum = true)
@MongoEntity(collection = "userClaims")
public class UserClaims {

  @BsonId private String uid;
  @JsonIgnore private String tenantId;
  private String email;
  private String name;
  private String familyName;
  @Builder.Default private List<ProductRoles> productRoles = List.of();
  private boolean test;
}
