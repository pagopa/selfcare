package it.pagopa.selfcare.iam.repository;

import static org.junit.jupiter.api.Assertions.*;

import com.mongodb.client.model.IndexOptions;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.iam.entity.UserClaims;
import it.pagopa.selfcare.iam.exception.ConflictException;
import it.pagopa.selfcare.iam.model.ProductRoles;
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
class UserClaimsRepositoryTest {

  @Inject UserClaimsRepository repository;
  @Inject TenantContext tenantContext;
  @Inject TenantMongoClientProducer tenantMongoClientProducer;

  @BeforeEach
  void clean() {
    tenantContext.setTenantId("AR");
    collection().deleteMany(new Document()).await().indefinitely();
  }

  @Test
  void upsert_shouldStoreTenantOnlyInProductRoles() {
    repository.upsert(user("uid-1", "enc", role("prod-a", "AR"))).await().indefinitely();

    Document stored =
        collection().find(new Document("_id", "uid-1")).collect().first().await().indefinitely();
    assertFalse(stored.containsKey("tenantId"));
    assertEquals("AR", stored.getList("productRoles", Document.class).get(0).getString("tenantId"));
  }

  @Test
  void identity_shouldBeSharedByEveryTenant() {
    repository.upsert(user("uid-1", "enc", role("prod-a", "AR"))).await().indefinitely();

    tenantContext.setTenantId("PNPG");
    assertEquals("uid-1", repository.findByUid("uid-1").await().indefinitely().getUid());
    assertEquals("uid-1", repository.findByEmail("enc").await().indefinitely().getUid());

    UserClaims updated = user("uid-1", "enc", role("prod-a", "AR"), role("prod-b", "PNPG"));
    updated.setName("updated");
    repository.upsert(updated).await().indefinitely();

    tenantContext.setTenantId("AR");
    UserClaims result = repository.findByUid("uid-1").await().indefinitely();
    assertEquals("updated", result.getName());
    assertEquals(2, result.getProductRoles().size());
    assertEquals(1L, collection().countDocuments().await().indefinitely());
  }

  @Test
  void upsert_shouldFailWhenEmailBelongsToAnotherUser() {
    collection()
        .createIndex(new Document("email", 1), new IndexOptions().unique(true))
        .await()
        .indefinitely();
    repository.upsert(user("uid-1", "enc", role("prod-a", "AR"))).await().indefinitely();

    assertThrows(
        ConflictException.class,
        () -> repository.upsert(user("uid-2", "enc", role("prod-a", "AR"))).await().indefinitely());
  }

  @Test
  void productLookups_shouldMatchOnlyRolesOfCurrentTenant() {
    collection()
        .insertOne(
            new Document("_id", "uid-2")
                .append(
                    "productRoles",
                    List.of(
                        new Document("productId", "prod-a").append("tenantId", "PNPG"),
                        new Document("productId", "ALL").append("tenantId", "AR"))))
        .await()
        .indefinitely();

    UserClaims result = repository.findByUidAndProductId("uid-2", "prod-a").await().indefinitely();
    assertNotNull(result);
    assertTrue(repository.findByProductId("prod-a").await().indefinitely().isEmpty());
    assertEquals(1, repository.findByProductId("ALL").await().indefinitely().size());

    tenantContext.setTenantId("PNPG");
    assertNotNull(repository.findByUidAndProductId("uid-2", "prod-a").await().indefinitely());
    assertEquals(1, repository.findByProductId("prod-a").await().indefinitely().size());
    assertTrue(repository.findByProductId("ALL").await().indefinitely().isEmpty());
    assertNull(repository.findByUidAndProductId("uid-2", "prod-b").await().indefinitely());
  }

  @Test
  void legacyProductRoleWithoutTenant_shouldBeVisibleWhenNotStrict() {
    collection()
        .insertOne(
            new Document("_id", "uid-legacy")
                .append("productRoles", List.of(new Document("productId", "prod-l"))))
        .await()
        .indefinitely();

    assertEquals(1, repository.findByProductId("prod-l").await().indefinitely().size());
    tenantContext.setTenantId("PNPG");
    assertNotNull(repository.findByUidAndProductId("uid-legacy", "prod-l").await().indefinitely());
  }

  @Test
  void findByProductId_shouldReturnEmptyWithoutProduct() {
    assertTrue(repository.findByProductId(null).await().indefinitely().isEmpty());
  }

  private io.quarkus.mongodb.reactive.ReactiveMongoCollection<Document> collection() {
    return tenantMongoClientProducer.databaseForCurrentTenant().getCollection("userClaims");
  }

  private static ProductRoles role(String productId, String tenantId) {
    return ProductRoles.builder()
        .productId(productId)
        .tenantId(tenantId)
        .roles(List.of("admin"))
        .build();
  }

  private static UserClaims user(String uid, String email, ProductRoles... productRoles) {
    return UserClaims.builder()
        .uid(uid)
        .email(email)
        .name("name")
        .productRoles(List.of(productRoles))
        .build();
  }
}
