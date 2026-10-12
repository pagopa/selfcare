package it.pagopa.selfcare.iam.repository;

import static org.junit.jupiter.api.Assertions.*;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import it.pagopa.selfcare.iam.entity.UserClaims;
import it.pagopa.selfcare.iam.exception.ConflictException;
import it.pagopa.selfcare.iam.testsupport.TenantMongoTestResource;
import it.pagopa.selfcare.tenant.TenantContext;
import it.pagopa.selfcare.tenant.mongodb.TenantMongoClientProducer;
import jakarta.inject.Inject;
import java.util.Map;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(UserClaimsRepositoryStrictTest.StrictProfile.class)
@QuarkusTestResource(value = TenantMongoTestResource.class, restrictToAnnotatedClass = true)
class UserClaimsRepositoryStrictTest {

  public static class StrictProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
      return Map.of("selfcare.tenant.strict-data-isolation", "true");
    }
  }

  @Inject UserClaimsRepository repository;
  @Inject TenantContext tenantContext;
  @Inject TenantMongoClientProducer tenantMongoClientProducer;

  @BeforeEach
  void clean() {
    tenantContext.setTenantId("AR");
    collection().deleteMany(new Document()).await().indefinitely();
  }

  @Test
  void legacyDocumentWithoutTenant_shouldBeHidden() {
    collection()
        .insertOne(new Document("_id", "uid-legacy").append("email", "enc-legacy"))
        .await()
        .indefinitely();

    assertNull(repository.findByEmail("enc-legacy").await().indefinitely());
    assertThrows(
        ConflictException.class,
        () ->
            repository
                .upsert(UserClaims.builder().uid("uid-legacy").email("enc-legacy").build())
                .await()
                .indefinitely());
  }

  @Test
  void upsert_shouldInsertAndUpdateTenantDocument() {
    repository
        .upsert(UserClaims.builder().uid("uid-1").email("enc").build())
        .await()
        .indefinitely();
    repository
        .upsert(UserClaims.builder().uid("uid-1").email("enc").name("updated").build())
        .await()
        .indefinitely();

    UserClaims result = repository.findByEmail("enc").await().indefinitely();
    assertEquals("AR", result.getTenantId());
    assertEquals("updated", result.getName());
  }

  private io.quarkus.mongodb.reactive.ReactiveMongoCollection<Document> collection() {
    return tenantMongoClientProducer.databaseForCurrentTenant().getCollection("userClaims");
  }
}
