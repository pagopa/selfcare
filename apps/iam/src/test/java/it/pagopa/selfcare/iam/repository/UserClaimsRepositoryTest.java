package it.pagopa.selfcare.iam.repository;

import static org.junit.jupiter.api.Assertions.*;

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
    for (String tenant : List.of("PNPG", "AR")) {
      tenantContext.setTenantId(tenant);
      collection().deleteMany(new Document()).await().indefinitely();
    }
  }

  @Test
  void upsert_shouldWriteIntoTenantDatabaseAndSetTenantOnInsert() {
    repository.upsert(user("uid-1", "enc-ar")).await().indefinitely();

    Document stored =
        collection().find(new Document("_id", "uid-1")).collect().first().await().indefinitely();
    assertEquals("AR", stored.getString("tenantId"));
    assertEquals("AR", repository.findByUid("uid-1").await().indefinitely().getTenantId());

    tenantContext.setTenantId("PNPG");
    assertNull(repository.findByUid("uid-1").await().indefinitely());
    assertEquals(0L, collection().countDocuments().await().indefinitely());
  }

  @Test
  void upsert_shouldUpdateWithoutChangingTenant() {
    repository.upsert(user("uid-1", "enc-ar")).await().indefinitely();
    UserClaims updated = user("uid-1", "enc-ar");
    updated.setName("updated");

    repository.upsert(updated).await().indefinitely();

    UserClaims result = repository.findByUid("uid-1").await().indefinitely();
    assertEquals("updated", result.getName());
    assertEquals("AR", result.getTenantId());
  }

  @Test
  void interleavedTenants_shouldKeepDataSeparated() {
    repository.upsert(user("uid-1", "enc-ar")).await().indefinitely();
    tenantContext.setTenantId("PNPG");
    repository.upsert(user("uid-1", "enc-pnpg")).await().indefinitely();
    tenantContext.setTenantId("AR");
    UserClaims ar = repository.findByEmail("enc-ar").await().indefinitely();
    assertNull(repository.findByEmail("enc-pnpg").await().indefinitely());
    tenantContext.setTenantId("PNPG");
    UserClaims pnpg = repository.findByEmail("enc-pnpg").await().indefinitely();
    assertNull(repository.findByEmail("enc-ar").await().indefinitely());

    assertEquals("AR", ar.getTenantId());
    assertEquals("PNPG", pnpg.getTenantId());
  }

  @Test
  void documentOfAnotherTenant_shouldBeInvisibleAndNotOverwritten() {
    collection()
        .insertOne(new Document("_id", "uid-x").append("tenantId", "PNPG").append("email", "enc-x"))
        .await()
        .indefinitely();

    assertNull(repository.findByUid("uid-x").await().indefinitely());
    assertNull(repository.findByEmail("enc-x").await().indefinitely());
    assertThrows(
        ConflictException.class,
        () -> repository.upsert(user("uid-x", "enc-ar")).await().indefinitely());

    Document stored =
        collection().find(new Document("_id", "uid-x")).collect().first().await().indefinitely();
    assertEquals("PNPG", stored.getString("tenantId"));
    assertEquals("enc-x", stored.getString("email"));
  }

  @Test
  void legacyDocumentWithoutTenant_shouldBeVisibleWhenNotStrict() {
    collection()
        .insertOne(new Document("_id", "uid-legacy").append("email", "enc-legacy"))
        .await()
        .indefinitely();

    assertNotNull(repository.findByEmail("enc-legacy").await().indefinitely());
  }

  @Test
  void findByUidAndProductId_shouldMatchOnlyRolesOfCurrentTenant() {
    collection()
        .insertOne(
            new Document("_id", "uid-2")
                .append("tenantId", "AR")
                .append(
                    "productRoles",
                    List.of(
                        new Document("productId", "prod-a").append("tenantId", "PNPG"),
                        new Document("productId", "ALL").append("tenantId", "AR"))))
        .await()
        .indefinitely();

    UserClaims result = repository.findByUidAndProductId("uid-2", "prod-a").await().indefinitely();
    assertNotNull(result);
    assertTrue(result.getProductRoles().stream().anyMatch(pr -> "ALL".equals(pr.getProductId())));

    assertTrue(repository.findByProductId("prod-a").await().indefinitely().isEmpty());
    assertEquals(1, repository.findByProductId("ALL").await().indefinitely().size());
  }

  private io.quarkus.mongodb.reactive.ReactiveMongoCollection<Document> collection() {
    return tenantMongoClientProducer.databaseForCurrentTenant().getCollection("userClaims");
  }

  private static UserClaims user(String uid, String email) {
    return UserClaims.builder()
        .uid(uid)
        .email(email)
        .name("name")
        .productRoles(
            List.of(ProductRoles.builder().productId("prod-a").roles(List.of("admin")).build()))
        .build();
  }
}
