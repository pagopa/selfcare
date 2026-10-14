package it.pagopa.selfcare.iam.repository;

import com.mongodb.ErrorCategory;
import com.mongodb.MongoWriteException;
import com.mongodb.client.model.UpdateOptions;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepositoryBase;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.iam.entity.UserClaims;
import it.pagopa.selfcare.iam.exception.ConflictException;
import it.pagopa.selfcare.iam.model.ProductRoles;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Optional;
import org.bson.Document;

/**
 * Access to the userClaims collection. The identity is shared by every tenant; product role lookups
 * are restricted to the current tenant through {@link TenantDataFilter}.
 */
@ApplicationScoped
public class UserClaimsRepository
    implements ReactivePanacheMongoRepositoryBase<UserClaims, String> {

  public static final String ALL = "ALL";

  private final TenantDataFilter tenantDataFilter;

  @Inject
  public UserClaimsRepository(TenantDataFilter tenantDataFilter) {
    this.tenantDataFilter = tenantDataFilter;
  }

  public Uni<UserClaims> findByUid(String uid) {
    return find(new Document("_id", uid)).firstResult();
  }

  public Uni<UserClaims> findByEmail(String email) {
    return find(new Document("email", email)).firstResult();
  }

  /**
   * Finds the user having a role on {@code productId} in the current tenant, falling back to a role
   * on every product ({@code ALL}).
   */
  public Uni<UserClaims> findByUidAndProductId(String uid, String productId) {
    if (productId == null) {
      return findByUid(uid);
    }
    return findByUidAndProductRole(uid, productId)
        .onItem()
        .ifNull()
        .switchTo(() -> findByUidAndProductRole(uid, ALL));
  }

  public Uni<List<UserClaims>> findByProductId(String productId) {
    if (productId == null) {
      return Uni.createFrom().item(List.of());
    }
    return find(productRoleMatch(productId)).list();
  }

  /**
   * Inserts or updates the user. {@code user} must already hold the product roles of every tenant
   * (see {@code IamServiceImpl#saveUser}); an email already bound to another uid results in a
   * {@link ConflictException}.
   */
  public Uni<Void> upsert(UserClaims user) {
    Document filter = new Document("_id", user.getUid());
    Document update =
        new Document(
            "$set",
            new Document(UserClaims.Fields.email.name(), user.getEmail())
                .append(UserClaims.Fields.name.name(), user.getName())
                .append(UserClaims.Fields.familyName.name(), user.getFamilyName())
                .append(
                    UserClaims.Fields.productRoles.name(),
                    Optional.ofNullable(user.getProductRoles()).orElse(List.of()).stream()
                        .map(this::toDocument)
                        .toList())
                .append(UserClaims.Fields.test.name(), user.isTest()));
    return mongoCollection()
        .updateOne(filter, update, new UpdateOptions().upsert(true))
        .onFailure(UserClaimsRepository::isDuplicateKey)
        .transform(failure -> new ConflictException("User already exists"))
        .replaceWithVoid();
  }

  private Uni<UserClaims> findByUidAndProductRole(String uid, String productId) {
    return find(productRoleMatch(productId).append("_id", uid)).firstResult();
  }

  private Document productRoleMatch(String productId) {
    return new Document(
        UserClaims.Fields.productRoles.name(),
        new Document(
            "$elemMatch",
            new Document(
                "$and",
                List.of(
                    new Document("productId", productId),
                    tenantDataFilter.productRoleFilter(null)))));
  }

  private Document toDocument(ProductRoles productRoles) {
    return new Document("productId", productRoles.getProductId())
        .append(TenantDataFilter.TENANT_ID, productRoles.getTenantId())
        .append("roles", productRoles.getRoles());
  }

  private static boolean isDuplicateKey(Throwable failure) {
    return failure instanceof MongoWriteException writeException
        && writeException.getError().getCategory() == ErrorCategory.DUPLICATE_KEY;
  }
}
