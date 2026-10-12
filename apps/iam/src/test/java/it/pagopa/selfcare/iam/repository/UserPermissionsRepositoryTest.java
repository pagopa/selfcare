package it.pagopa.selfcare.iam.repository;

import static org.junit.jupiter.api.Assertions.*;

import io.quarkus.mongodb.reactive.ReactiveMongoDatabase;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.iam.exception.ResourceNotFoundException;
import it.pagopa.selfcare.iam.model.ProductRole;
import it.pagopa.selfcare.iam.model.ProductRolePermissions;
import it.pagopa.selfcare.iam.testsupport.TenantMongoTestResource;
import it.pagopa.selfcare.tenant.TenantContext;
import it.pagopa.selfcare.tenant.mongodb.TenantMongoClientProducer;
import jakarta.inject.Inject;
import java.util.List;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(value = TenantMongoTestResource.class, restrictToAnnotatedClass = true)
class UserPermissionsRepositoryTest {

  @Inject UserPermissionsRepository repository;
  @Inject TenantContext tenantContext;
  @Inject TenantMongoClientProducer tenantMongoClientProducer;

  @BeforeEach
  void setUp() {
    for (String tenant : List.of("PNPG", "AR")) {
      tenantContext.setTenantId(tenant);
      database().getCollection("userClaims").deleteMany(new Document()).await().indefinitely();
      database().getCollection("roles").deleteMany(new Document()).await().indefinitely();
      database()
          .getCollection("roles")
          .insertOne(
              new Document("_id", "admin")
                  .append("group", "g")
                  .append("permissions", List.of("read:users")))
          .await()
          .indefinitely();
    }
    // Same uid in both tenants: AR document also embeds a PNPG role that must stay invisible.
    tenantContext.setTenantId("AR");
    insertUser("AR", List.of(role("prod-ar", "AR"), role("prod-x", "PNPG")));
    tenantContext.setTenantId("PNPG");
    insertUser("PNPG", List.of(role("prod-pnpg", null)));
  }

  @Test
  void getUserProductRoles_shouldReturnOnlyRolesOfCurrentTenant() {
    tenantContext.setTenantId("AR");
    List<ProductRole> ar = repository.getUserProductRoles("uid-1", null).await().indefinitely();
    tenantContext.setTenantId("PNPG");
    List<ProductRole> pnpg = repository.getUserProductRoles("uid-1", null).await().indefinitely();

    assertEquals(List.of("prod-ar"), ar.stream().map(ProductRole::getProductId).toList());
    assertEquals(List.of("prod-pnpg"), pnpg.stream().map(ProductRole::getProductId).toList());
    assertEquals("g", ar.get(0).getRoles().get(0).getGroup());
  }

  @Test
  void getUserPermissions_shouldIgnoreRolesOfOtherTenants() {
    tenantContext.setTenantId("AR");
    assertEquals(
        List.of("read:users"),
        repository
            .getUserPermissions("uid-1", "read:users", List.of("prod-ar"))
            .await()
            .indefinitely()
            .getPermissions());
    assertThrows(
        ResourceNotFoundException.class,
        () ->
            repository
                .getUserPermissions("uid-1", "read:users", List.of("prod-x"))
                .await()
                .indefinitely());
  }

  @Test
  void getUserProductRolePermissionsList_shouldApplyTenantBeforeLimit() {
    tenantContext.setTenantId("AR");
    database()
        .getCollection("userClaims")
        .updateOne(
            new Document("_id", "uid-1"),
            new Document(
                "$set",
                new Document("productRoles", List.of(role("prod-y", "PNPG"), role("ALL", "AR")))))
        .await()
        .indefinitely();

    List<ProductRolePermissions> result =
        repository.getUserProductRolePermissionsList("uid-1", "prod-y").await().indefinitely();

    assertEquals(1, result.size());
    assertEquals("ALL", result.get(0).getProductId());
  }

  private void insertUser(String tenantId, List<Document> productRoles) {
    database()
        .getCollection("userClaims")
        .insertOne(
            new Document("_id", "uid-1")
                .append("tenantId", tenantId)
                .append("email", "enc")
                .append("productRoles", productRoles))
        .await()
        .indefinitely();
  }

  private static Document role(String productId, String tenantId) {
    return new Document("productId", productId)
        .append("tenantId", tenantId)
        .append("roles", List.of("admin"));
  }

  private ReactiveMongoDatabase database() {
    return tenantMongoClientProducer.databaseForCurrentTenant();
  }
}
