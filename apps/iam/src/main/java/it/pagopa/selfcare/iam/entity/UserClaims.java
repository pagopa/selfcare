package it.pagopa.selfcare.iam.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntityBase;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.iam.model.ProductRoles;
import it.pagopa.selfcare.iam.service.CurrentTenantProvider;
import jakarta.enterprise.inject.spi.CDI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.bson.codecs.pojo.annotations.BsonId;
import org.eclipse.microprofile.config.ConfigProvider;

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
  @JsonIgnore
  private String tenantId;
  private String email;
  private String name;
  private String familyName;
  @Builder.Default private List<ProductRoles> productRoles = List.of();
  private boolean test;

  /**
   * Appends the tenant predicate to a positional-parameter query, adding the tenant as the next
   * positional argument.
   *
   * <p><b>Migration phase:</b> the predicate matches the current tenant <em>or</em> documents with
   * no tenant at all. Documents written before the discriminator existed carry none, and a strict
   * equality would make every one of them invisible to both tenants. Once the backfill has tagged
   * every document {@code selfcare.tenant.strict-data-isolation} drops that branch.
   *
   * <p>When no tenant is resolvable the query is left unscoped rather than made unsatisfiable. That
   * is the pre-multitenant behaviour for callers outside an active request and is a migration-phase
   * concession, not a security boundary.
   */
  private static String tenantScoped(String query, List<Object> params) {
    Optional<String> tenantId = currentTenantId();
    if (tenantId.isEmpty()) {
      return query;
    }
    params.add(tenantId.get());
    return strictTenantIsolation()
        ? query + " and tenantId = ?" + params.size()
        : query + " and (tenantId = ?" + params.size() + " or tenantId is null)";
  }

  /**
   * Whether untagged documents are still treated as belonging to the current tenant.
   *
   * <p>Configuration rather than a code constant because the backfill runs at a different time in
   * each environment; see {@code Step_1/EPIC.md} sub-tasks 2 and 10. Read through the static config
   * API for the same reason the tenant provider is read through {@code CDI.current()}: this
   * predicate is built from a static context. Defaults to the lenient behaviour, and both the flag
   * and the {@code or tenantId is null} branch must be deleted once every environment runs strict.
   */
  private static boolean strictTenantIsolation() {
    return ConfigProvider.getConfig()
        .getOptionalValue("selfcare.tenant.strict-data-isolation", Boolean.class)
        .orElse(false);
  }

  private static Object[] args(List<Object> params) {
    return params.toArray();
  }

  private static Optional<String> currentTenantId() {
    return CDI.current().select(CurrentTenantProvider.class).get().currentTenantId();
  }

  private void stampTenantIfAbsent() {
    if (tenantId == null) {
      currentTenantId().ifPresent(this::setTenantId);
    }
  }

  @Override
  public <T extends ReactivePanacheMongoEntityBase> Uni<T> persist() {
    stampTenantIfAbsent();
    return super.persist();
  }

  @Override
  public <T extends ReactivePanacheMongoEntityBase> Uni<T> update() {
    stampTenantIfAbsent();
    return super.update();
  }

  @Override
  public <T extends ReactivePanacheMongoEntityBase> Uni<T> persistOrUpdate() {
    stampTenantIfAbsent();
    return super.persistOrUpdate();
  }

  public static Uni<UserClaims> findByUid(String uid) {
    List<Object> params = new ArrayList<>(List.of(uid));
    String query = tenantScoped("_id = ?1", params);
    return find(query, args(params)).firstResult().map(entity -> (UserClaims) entity);
  }

  public static Uni<UserClaims> findByEmail(String email) {
    List<Object> params = new ArrayList<>(List.of(email));
    String query = tenantScoped("email = ?1", params);
    return find(query, args(params)).firstResult().map(entity -> (UserClaims) entity);
  }

  public static Uni<UserClaims> findByUidAndProductId(String uid, String productId) {
    return Optional.ofNullable(productId)
        .map(
            pid -> {
              List<Object> params = new ArrayList<>(List.of(uid, pid));
              String query = tenantScoped("_id = ?1 and productRoles.productId = ?2", params);
              return find(query, args(params))
                  .firstResult()
                  .map(entity -> (UserClaims) entity)
                  .onItem()
                  .ifNull()
                  .switchTo(
                      () -> {
                        List<Object> fallbackParams = new ArrayList<>(List.of(uid, "ALL"));
                        String fallbackQuery =
                            tenantScoped(
                                "_id = ?1 and productRoles.productId = ?2", fallbackParams);
                        return find(fallbackQuery, args(fallbackParams))
                            .firstResult()
                            .map(entity -> (UserClaims) entity);
                      });
            })
        .orElseGet(() -> findByUid(uid));
  }

  public static Uni<List<UserClaims>> findByProductId(String productId) {
    return Optional.ofNullable(productId)
        .map(
            pid -> {
              List<Object> params = new ArrayList<>(List.of(pid));
              String query = tenantScoped("productRoles.productId = ?1", params);
              return find(query, args(params))
                  .list()
                  .map(list -> list.stream().map(entity -> (UserClaims) entity).toList());
            })
        .orElseGet(() -> Uni.createFrom().item(List.of()));
  }
}
