package it.pagopa.selfcare.iam.entity;

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
 *   <li>Unique: email
 *   <li>Secondary: (productRoles.tenantId, productRoles.productId)
 * </ul>
 *
 * <h2>Tenant</h2>
 *
 * <p>The identity (uid, email, name) is shared by every tenant: one document per email. Each
 * embedded {@link ProductRoles} carries the {@code tenantId} that enables the product, and only the
 * roles of the current tenant are read or written (see {@link
 * it.pagopa.selfcare.iam.repository.TenantDataFilter}).
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
  private String email;
  private String name;
  private String familyName;
  @Builder.Default private List<ProductRoles> productRoles = List.of();
  private boolean test;
}
