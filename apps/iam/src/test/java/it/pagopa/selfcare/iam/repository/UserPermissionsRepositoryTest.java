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
import java.util.Set;
import java.util.stream.Collectors;
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
    tenantContext.setTenantId("AR");
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
    // One identity shared by both tenants; each role is visible only in its own tenant.
    insertUser(List.of(role("prod-ar", "AR"), role("prod-x", "PNPG"), role("prod-legacy", null)));
  }

  @Test
  void getUserProductRoles_shouldReturnOnlyRolesOfCurrentTenant() {
    tenantContext.setTenantId("AR");
    List<ProductRole> ar = repository.getUserProductRoles("uid-1", null).await().indefinitely();
    tenantContext.setTenantId("PNPG");
    List<ProductRole> pnpg = repository.getUserProductRoles("uid-1", null).await().indefinitely();

    assertEquals(
        Set.of("prod-ar", "prod-legacy"),
        ar.stream().map(ProductRole::getProductId).collect(Collectors.toSet()));
    assertEquals(
        Set.of("prod-x", "prod-legacy"),
        pnpg.stream().map(ProductRole::getProductId).collect(Collectors.toSet()));
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

  private void insertUser(List<Document> productRoles) {
    database()
        .getCollection("userClaims")
        .insertOne(
            new Document("_id", "uid-1")
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
