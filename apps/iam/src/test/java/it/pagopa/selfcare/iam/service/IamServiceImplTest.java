package it.pagopa.selfcare.iam.service;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.iam.controller.request.SaveUserRequest;
import it.pagopa.selfcare.iam.entity.UserClaims;
import it.pagopa.selfcare.iam.exception.InternalException;
import it.pagopa.selfcare.iam.exception.InvalidRequestException;
import it.pagopa.selfcare.iam.exception.ResourceNotFoundException;
import it.pagopa.selfcare.iam.model.ProductRolePermissions;
import it.pagopa.selfcare.iam.model.ProductRolePermissionsList;
import it.pagopa.selfcare.iam.model.ProductRoles;
import it.pagopa.selfcare.iam.model.UserPermissions;
import it.pagopa.selfcare.iam.repository.UserPermissionsRepository;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@QuarkusTest
class IamServiceImplTest {

  @Inject
  IamServiceImpl service;

  @InjectMock
  UserPermissionsRepository userPermissionsRepository;

  @Test
  void ping_shouldReturnOK() {
    String result = service.ping().await().indefinitely();
    assertEquals("OK", result);
  }

  // ========== saveUser Tests ==========

  @Test
  void saveUser_shouldThrowInvalidRequestException_whenRequestIsNull() {
    assertThrows(InvalidRequestException.class,
      () -> service.saveUser(null, "productA").await().indefinitely());
  }

  @Test
  void saveUser_shouldThrowInvalidRequestException_whenEmailIsNull() {
    SaveUserRequest request = new SaveUserRequest();
    request.setEmail(null);
    request.setName("John");

    assertThrows(InvalidRequestException.class,
      () -> service.saveUser(request, "productA").await().indefinitely());
  }

  @Test
  void saveUser_shouldThrowInvalidRequestException_whenEmailIsBlank() {
    SaveUserRequest request = new SaveUserRequest();
    request.setEmail("   ");
    request.setName("John");

    assertThrows(InvalidRequestException.class,
      () -> service.saveUser(request, "productA").await().indefinitely());
  }

  @Test
  void saveUser_shouldCreateNewUser_whenUserDoesNotExist() {
    SaveUserRequest request = new SaveUserRequest();
    request.setEmail("new@example.com");
    request.setName("John");
    request.setFamilyName("Doe");
    request.setProductRoles(List.of(
      ProductRoles.builder().productId("productA").roles(List.of("admin")).build()
    ));

    try (MockedStatic<UserClaims> mockedStatic = Mockito.mockStatic(UserClaims.class)) {
      mockedStatic.when(UserClaims::builder).thenCallRealMethod();
      mockedStatic.when(() -> UserClaims.findByEmail("new@example.com"))
        .thenReturn(Uni.createFrom().nullItem());

      UserClaims result = service.saveUser(request, "productA").await().indefinitely();

      assertNotNull(result);
      assertEquals("new@example.com", result.getEmail());
      assertEquals("John", result.getName());
      assertEquals("Doe", result.getFamilyName());
      assertNotNull(result.getUid());
      assertEquals(1, result.getProductRoles().size());
      assertEquals("productA", result.getProductRoles().get(0).getProductId());
      assertTrue(result.getProductRoles().get(0).getRoles().contains("admin"));
    }
  }

  @Test
  void saveUser_shouldUpdateExistingUser_whenUserExists() {
    SaveUserRequest request = new SaveUserRequest();
    request.setEmail("existing@example.com");
    request.setName("Jane");
    request.setFamilyName("Smith");
    request.setProductRoles(List.of(
      ProductRoles.builder().productId("productA").roles(List.of("operator")).build()
    ));

    String existingUid = UUID.randomUUID().toString();
    UserClaims existingUser = UserClaims.builder()
      .email("existing@example.com")
      .uid(existingUid)
      .name("OldName")
      .familyName("OldFamily")
      .productRoles(List.of(
        ProductRoles.builder().productId("productA").roles(List.of("admin")).build()
      ))
      .build();

    UserClaims spyUser = spy(existingUser);

    try (MockedStatic<UserClaims> mockedStatic = Mockito.mockStatic(UserClaims.class)) {
      mockedStatic.when(UserClaims::builder).thenCallRealMethod();
      mockedStatic.when(() -> UserClaims.findByEmail("AEpJ4wSTGoucJLy8OCYfL23M0kSt0WwYo9/cJ9jboUtEgxsJ"))
        .thenReturn(Uni.createFrom().item(spyUser));

      UserClaims result = service.saveUser(request, "productA").await().indefinitely();

      assertNotNull(result);
      assertEquals(existingUid, result.getUid());
      assertEquals("Jane", result.getName());
      assertEquals("Smith", result.getFamilyName());
      verify(spyUser, times(1)).persistOrUpdate();
    }
  }

  @Test
  void saveUser_shouldMergeProductRoles_whenProductIdProvided() {
    SaveUserRequest request = new SaveUserRequest();
    request.setEmail("merge@example.com");
    request.setName("Bob");
    request.setProductRoles(List.of(
      ProductRoles.builder().productId("productB").roles(List.of("viewer")).build()
    ));

    String existingUid = UUID.randomUUID().toString();
    UserClaims existingUser = UserClaims.builder()
      .email("CFdS9xW6EZS9LLSxMHgQJS7KuMuQ50MOy5zX05Tjt3L5")
      .uid(existingUid)
      .name("Bob")
      .productRoles(List.of(
        ProductRoles.builder().productId("productA").roles(List.of("admin")).build()
      ))
      .build();

    UserClaims spyUser = spy(existingUser);

    try (MockedStatic<UserClaims> mockedStatic = Mockito.mockStatic(UserClaims.class)) {
      mockedStatic.when(UserClaims::builder).thenCallRealMethod();
      mockedStatic.when(() -> UserClaims.findByEmail("CFdS9xW6EZS9LLSxMHgQJS7KuMuQ50MOy5zX05Tjt3L5"))
        .thenReturn(Uni.createFrom().item(spyUser));

      UserClaims result = service.saveUser(request, "productB").await().indefinitely();

      assertNotNull(result);
      assertEquals(2, spyUser.getProductRoles().size());
      assertTrue(spyUser.getProductRoles().stream()
        .anyMatch(pr -> pr.getProductId().equals("productA")));
      assertTrue(spyUser.getProductRoles().stream()
        .anyMatch(pr -> pr.getProductId().equals("productB")));
    }
  }

  @Test
  void saveUser_shouldReplaceProductRoles_whenProductIdMatchesExisting() {
    SaveUserRequest request = new SaveUserRequest();
    request.setEmail("replace@example.com");
    request.setName("Alice");
    request.setProductRoles(List.of(
      ProductRoles.builder().productId("productA").roles(List.of("superadmin")).build()
    ));

    String existingUid = UUID.randomUUID().toString();
    UserClaims existingUser = UserClaims.builder()
      .email("replace@example.com")
      .uid(existingUid)
      .name("Alice")
      .productRoles(List.of(
        ProductRoles.builder().productId("productA").roles(List.of("admin", "operator")).build()
      ))
      .build();

    UserClaims spyUser = spy(existingUser);

    try (MockedStatic<UserClaims> mockedStatic = Mockito.mockStatic(UserClaims.class)) {
      mockedStatic.when(UserClaims::builder).thenCallRealMethod();
      mockedStatic.when(() -> UserClaims.findByEmail("F1dQ/BGZEay5OaWwJToWZCDA0H9W7CtYk084i1dg1crtqEc="))
        .thenReturn(Uni.createFrom().item(spyUser));

      service.saveUser(request, "productA").await().indefinitely();

      assertEquals(1, spyUser.getProductRoles().size());
      assertEquals("productA", spyUser.getProductRoles().get(0).getProductId());
      assertEquals(1, spyUser.getProductRoles().get(0).getRoles().size());
      assertTrue(spyUser.getProductRoles().get(0).getRoles().contains("superadmin"));
    }
  }

  @Test
  void saveUser_shouldReplaceAllProductRoles_whenProductIdIsNull() {
    SaveUserRequest request = new SaveUserRequest();
    request.setEmail("replaceall@example.com");
    request.setName("Charlie");
    request.setProductRoles(List.of(
      ProductRoles.builder().productId("productC").roles(List.of("newrole")).build()
    ));

    String existingUid = UUID.randomUUID().toString();
    UserClaims existingUser = UserClaims.builder()
      .email("replaceall@example.com")
      .uid(existingUid)
      .name("Charlie")
      .productRoles(List.of(
        ProductRoles.builder().productId("productA").roles(List.of("admin")).build(),
        ProductRoles.builder().productId("productB").roles(List.of("viewer")).build()
      ))
      .build();

    UserClaims spyUser = spy(existingUser);

    try (MockedStatic<UserClaims> mockedStatic = Mockito.mockStatic(UserClaims.class)) {
      mockedStatic.when(UserClaims::builder).thenCallRealMethod();
      mockedStatic.when(() -> UserClaims.findByEmail("F1dQ/BGZEY2wLYS4LTceOi/Kk0rnREus5wvMa9zMPRSkuA26a5Y="))
        .thenReturn(Uni.createFrom().item(spyUser));

      service.saveUser(request, null).await().indefinitely();

      assertEquals(1, spyUser.getProductRoles().size());
      assertEquals("productC", spyUser.getProductRoles().get(0).getProductId());
    }
  }

  // ========== getUser Tests ==========

  @Test
  void getUser_shouldReturnUser_whenUserExists() {
    String userId = "user-123";
    String productId = "productA";

    UserClaims foundUser = UserClaims.builder()
      .email("A11V/hS6EZS9LLSxMHgQJS6O1OStWkFnwpbN4fHhy0I6") //found@example.com
      .uid(userId)
      .productRoles(List.of(
        ProductRoles.builder().productId("productA").roles(List.of("admin")).build(),
        ProductRoles.builder().productId("productB").roles(List.of("viewer")).build()
      ))
      .build();

    try (MockedStatic<UserClaims> mockedStatic = Mockito.mockStatic(UserClaims.class)) {
      mockedStatic.when(UserClaims::builder).thenCallRealMethod();
      mockedStatic.when(() -> UserClaims.findByUidAndProductId(userId, productId))
        .thenReturn(Uni.createFrom().item(foundUser));

      UserClaims result = service.getUser(userId, productId).await().indefinitely();

      assertNotNull(result);
      assertEquals(userId, result.getUid());
      assertEquals(1, result.getProductRoles().size());
      assertEquals("productA", result.getProductRoles().get(0).getProductId());
    }
  }

  @Test
  void getUser_shouldThrowResourceNotFoundException_whenUserNotFound() {
    String userId = "non-existing-user";
    String productId = "productA";

    try (MockedStatic<UserClaims> mockedStatic = Mockito.mockStatic(UserClaims.class)) {
      mockedStatic.when(() -> UserClaims.findByUidAndProductId(userId, productId))
        .thenReturn(Uni.createFrom().nullItem());

      assertThrows(ResourceNotFoundException.class,
        () -> service.getUser(userId, productId).await().indefinitely());
    }
  }

  @Test
  void getUser_shouldReturnAllProductRoles_whenProductIdIsNull() {
    String userId = "user-456";

    UserClaims foundUser = UserClaims.builder()
      .email("BF5M0BWCFYGsLaHzNjker68nsqTr/pizCvinopNUlQ==") //all@example.com
      .uid(userId)
      .productRoles(List.of(
        ProductRoles.builder().productId("productA").roles(List.of("admin")).build(),
        ProductRoles.builder().productId("productB").roles(List.of("viewer")).build()
      ))
      .build();

    try (MockedStatic<UserClaims> mockedStatic = Mockito.mockStatic(UserClaims.class)) {
      mockedStatic.when(UserClaims::builder).thenCallRealMethod();
      mockedStatic.when(() -> UserClaims.findByUidAndProductId(userId, null))
        .thenReturn(Uni.createFrom().item(foundUser));

      UserClaims result = service.getUser(userId, null).await().indefinitely();

      assertNotNull(result);
      assertEquals(2, result.getProductRoles().size());
    }
  }

  // ========== setFilteredProductRoles Tests ==========

  @Test
  void setFilteredProductRoles_shouldReturnOriginalList_whenProductIdIsNull() {
    List<ProductRoles> roles = List.of(
      ProductRoles.builder().productId("productA").roles(List.of("admin")).build(),
      ProductRoles.builder().productId("productB").roles(List.of("viewer")).build()
    );

    List<ProductRoles> result = service.setFilteredProductRoles(roles, null);

    assertSame(roles, result);
    assertEquals(2, result.size());
  }

  @Test
  void setFilteredProductRoles_shouldFilterByProductId() {
    List<ProductRoles> roles = List.of(
      ProductRoles.builder().productId("productA").roles(List.of("admin")).build(),
      ProductRoles.builder().productId("productB").roles(List.of("viewer")).build()
    );

    List<ProductRoles> result = service.setFilteredProductRoles(roles, "productB");

    assertEquals(1, result.size());
    assertEquals("productB", result.get(0).getProductId());
  }

  @Test
  void setFilteredProductRoles_shouldReturnEmptyList_whenNoMatch() {
    List<ProductRoles> roles = List.of(
      ProductRoles.builder().productId("productA").roles(List.of("admin")).build()
    );

    List<ProductRoles> result = service.setFilteredProductRoles(roles, "productC");

    assertTrue(result.isEmpty());
  }

  @Test
  void setFilteredProductRoles_shouldReturnEmptyList_whenRolesIsNull() {
    List<ProductRoles> result = service.setFilteredProductRoles(null, "productA");

    assertTrue(result.isEmpty());
  }

  // ========== getProductRolePermissionsList Tests ==========

  @Test
  void getProductRolePermissionsList_shouldReturnProductRolePermissionsList_whenUserExists() {
    String userId = "user-123";
    String productId = "productA";

    ProductRolePermissions prp1 = ProductRolePermissions.builder()
        .productId("productA")
        .role("admin")
        .permissions(List.of("read:users", "write:users"))
        .build();

    List<ProductRolePermissions> productRolePermissions = List.of(prp1);

    ProductRolePermissionsList productRolePermissionsList = new ProductRolePermissionsList(productRolePermissions);

    when(userPermissionsRepository.getUserProductRolePermissionsList(userId, productId))
            .thenReturn(Uni.createFrom().item(productRolePermissions));

    ProductRolePermissionsList result = service.getProductRolePermissionsList(userId, productId)
            .await().indefinitely();

    assertNotNull(result);
    assertEquals(1, result.getItems().size());
    assertEquals(productRolePermissionsList, result);
  }

  @Test
  void getProductRolePermissionsList_shouldReturnEmptyList_whenNoMatch() {
    String userId = "user-123";
    String productId = "productA";

    when(userPermissionsRepository.getUserProductRolePermissionsList(userId, productId))
            .thenReturn(Uni.createFrom().item(Collections.emptyList()));

    ProductRolePermissionsList result = service.getProductRolePermissionsList(userId, productId)
            .await().indefinitely();

    assertNotNull(result);
    assertTrue(result.getItems().isEmpty());
  }

  @Test
  void getProductRolePermissionsList_serviceError_throwsInternalException() {
    String userId = "user-123";
    String productId = "productA";

    when(userPermissionsRepository.getUserProductRolePermissionsList(userId, productId))
            .thenReturn(Uni.createFrom().failure(new InternalException("Database error")));

    Uni<ProductRolePermissionsList> result = service.getProductRolePermissionsList(userId, productId);

    result.subscribe()
            .withSubscriber(UniAssertSubscriber.create())
            .awaitFailure()
            .assertFailedWith(InternalException.class);
  }

  // ========== hasPermission Tests ==========

  @Test
  void hasPermission_shouldReturnTrue_whenUserHasPermission() {
    String userId = "user-789";
    String permission = "read:users";
    String productId = "productA";

    UserPermissions userPermissions = UserPermissions.builder()
      .email("user@example.com")
      .uid(userId)
      .productId(productId)
      .permissions(List.of("read:users", "write:users"))
      .build();

    when(userPermissionsRepository.getUserPermissions(userId, permission, productId))
      .thenReturn(Uni.createFrom().item(userPermissions));

    Boolean result = service.hasPermission(userId, permission, productId, null)
      .await().indefinitely();

    assertTrue(result);
  }

  @Test
  void hasPermission_shouldReturnFalse_whenUserDoesNotHavePermission() {
    String userId = "user-101";
    String permission = "delete:users";
    String productId = "productA";

    UserPermissions userPermissions = UserPermissions.builder()
      .email("user@example.com")
      .uid(userId)
      .productId(productId)
      .permissions(List.of("read:users", "write:users"))
      .build();

    when(userPermissionsRepository.getUserPermissions(userId, permission, productId))
      .thenReturn(Uni.createFrom().item(userPermissions));

    Boolean result = service.hasPermission(userId, permission, productId, null)
      .await().indefinitely();

    assertFalse(result);
  }

  @Test
  void hasPermission_shouldReturnFalse_whenPermissionsAreEmpty() {
    String userId = "user-202";
    String permission = "read:users";
    String productId = "productA";

    UserPermissions userPermissions = UserPermissions.builder()
      .email("user@example.com")
      .uid(userId)
      .productId(productId)
      .permissions(List.of())
      .build();

    when(userPermissionsRepository.getUserPermissions(userId, permission, productId))
      .thenReturn(Uni.createFrom().item(userPermissions));

    Boolean result = service.hasPermission(userId, permission, productId, null)
      .await().indefinitely();

    assertFalse(result);
  }
}
