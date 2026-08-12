package it.pagopa.selfcare.iam.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import it.pagopa.selfcare.iam.entity.Roles;
import it.pagopa.selfcare.iam.entity.UserClaims;
import it.pagopa.selfcare.iam.model.ProductRole;
import it.pagopa.selfcare.iam.model.ProductRoles;
import it.pagopa.selfcare.iam.service.CurrentTenantProvider;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Exercises tenant-scoped userClaims data access against a real MongoDB.
 *
 * <p>Mocking Panache would not catch a tenant predicate that compiles but matches no documents, so
 * these tests pin the migration-phase semantics at the data-access layer: current-tenant documents
 * are visible, other-tenant documents are hidden, legacy untagged documents remain visible, and
 * callers without a tenant keep the pre-multitenant unscoped behaviour.
 */
@QuarkusTest
@QuarkusTestResource(value = MongoTestResource.class, restrictToAnnotatedClass = true)
class UserClaimsTenantIsolationTest {

  @Inject UserPermissionsRepository userPermissionsRepository;

  @InjectMock CurrentTenantProvider currentTenantProvider;

  @BeforeEach
  void cleanUp() {
    UserClaims.deleteAll().await().indefinitely();
    Roles.deleteAll().await().indefinitely();
    actAs("AR");
  }

  private void actAs(String tenantId) {
    when(currentTenantProvider.currentTenantId()).thenReturn(Optional.ofNullable(tenantId));
  }

  private UserClaims persistUser(String uid, String tenantId) {
    return persistUser(uid, tenantId, "product-A", "admin");
  }

  private UserClaims persistUser(String uid, String tenantId, String productId, String role) {
    UserClaims userClaims =
        UserClaims.builder()
            .uid(uid)
            .tenantId(tenantId)
            .email(uid + "@example.com")
            .productRoles(
                List.of(ProductRoles.builder().productId(productId).roles(List.of(role)).build()))
            .build();
    userClaims.persist().await().indefinitely();
    return userClaims;
  }

  private void persistRole(String name, String permission) {
    Roles.builder()
        .name(name)
        .group("IAM")
        .permissions(List.of(permission))
        .build()
        .persist()
        .await()
        .indefinitely();
  }

  @Test
  void findByUid_returnsCurrentTenantDocumentAndStampsTenantOnWrite() {
    persistUser("ar-user", null);

    UserClaims found = UserClaims.findByUid("ar-user").await().indefinitely();

    assertNotNull(found);
    assertEquals("AR", found.getTenantId());
  }

  @Test
  void findByUid_doesNotReturnDocumentOfAnotherTenant() {
    persistUser("pnpg-user", "PNPG");

    assertNull(UserClaims.findByUid("pnpg-user").await().indefinitely());

    actAs("PNPG");
    assertNotNull(UserClaims.findByUid("pnpg-user").await().indefinitely());
  }

  @Test
  void findByUid_stillReturnsLegacyUntaggedDocument() {
    actAs(null);
    persistUser("legacy-user", null);

    actAs("AR");
    UserClaims found = UserClaims.findByUid("legacy-user").await().indefinitely();

    assertNotNull(found);
    assertNull(found.getTenantId());
  }

  @Test
  void withoutAResolvableTenantQueriesAreLeftUnscoped() {
    persistUser("pnpg-user", "PNPG");
    actAs(null);

    assertNotNull(UserClaims.findByUid("pnpg-user").await().indefinitely());
  }

  @Test
  void findByProductId_returnsOnlyCurrentTenantAndLegacyDocuments() {
    persistUser("ar-user", "AR");
    persistUser("pnpg-user", "PNPG");
    actAs(null);
    persistUser("legacy-user", null);

    actAs("AR");
    List<UserClaims> users = UserClaims.findByProductId("product-A").await().indefinitely();

    assertEquals(2, users.size());
    assertTrue(users.stream().anyMatch(user -> "ar-user".equals(user.getUid())));
    assertTrue(users.stream().anyMatch(user -> "legacy-user".equals(user.getUid())));
  }

  @Test
  void aggregationsAreScopedToTheCurrentTenant() {
    persistRole("admin", "read:users");
    persistUser("ar-user", "AR", "product-A", "admin");
    persistUser("pnpg-user", "PNPG", "product-A", "admin");

    List<ProductRole> currentTenantRoles =
        userPermissionsRepository
            .getUserProductRoles("ar-user", "product-A")
            .await()
            .indefinitely();
    List<ProductRole> otherTenantRoles =
        userPermissionsRepository
            .getUserProductRoles("pnpg-user", "product-A")
            .await()
            .indefinitely();

    assertEquals(1, currentTenantRoles.size());
    assertTrue(otherTenantRoles.isEmpty());

    actAs(null);
    assertEquals(
        1,
        userPermissionsRepository
            .getUserProductRoles("pnpg-user", "product-A")
            .await()
            .indefinitely()
            .size());
  }
}
