package it.pagopa.selfcare.user.util;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.entity.ProductRole;
import it.pagopa.selfcare.product.entity.ProductRoleInfo;
import it.pagopa.selfcare.product.service.ProductService;
import it.pagopa.selfcare.user.entity.UserInstitution;
import it.pagopa.selfcare.user.exception.InvalidRequestException;
import it.pagopa.selfcare.user.model.OnboardedProduct;
import it.pagopa.selfcare.user.model.UserNotificationToSend;
import it.pagopa.selfcare.user.model.constants.OnboardedProductState;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.apache.http.HttpStatus;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.openapi.quarkus.user_registry_json.model.*;

import java.util.*;
import java.util.function.BiFunction;

import static it.pagopa.selfcare.user.constant.CollectionUtil.MAIL_ID_PREFIX;
import static it.pagopa.selfcare.user.model.constants.OnboardedProductState.ACTIVE;
import static it.pagopa.selfcare.user.model.constants.OnboardedProductState.DELETED;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTest
class UserUtilTest {

    @Inject
    private UserUtils userUtils;

    @InjectMock
    private ProductService productService;

    private static UserResource getUserResource(UUID uuid) {
        Map<String, WorkContactResource> map = new HashMap<>();
        WorkContactResource workContactResource = new WorkContactResource();
        workContactResource.setEmail(EmailCertifiableSchema.builder().value("test@test.it").build());
        workContactResource.setMobilePhone(MobilePhoneCertifiableSchema.builder().value("0000000000").build());
        map.put("MAIL_ID#123", workContactResource);
        return UserResource.builder()
                .id(uuid)
                .name(NameCertifiableSchema.builder().value("name").build())
                .familyName(org.openapi.quarkus.user_registry_json.model.FamilyNameCertifiableSchema.builder().value("familyName").build())
                .workContacts(map)
                .build();
    }

    private static UserInstitution getUserInstitution(String userId, String institutionId, String productId) {
        UserInstitution userInstitution = new UserInstitution();
        userInstitution.setId(ObjectId.get());
        userInstitution.setUserId(userId);
        userInstitution.setInstitutionId(institutionId);
        userInstitution.setUserMailUuid("MAIL_ID#123");

        OnboardedProduct onboardedProduct = new OnboardedProduct();
        onboardedProduct.setProductId(productId);
        onboardedProduct.setStatus(ACTIVE);
        onboardedProduct.setRole(PartyRole.OPERATOR);
        onboardedProduct.setProductRole("admin2");

        OnboardedProduct onboardedProduct2 = new OnboardedProduct();
        onboardedProduct2.setProductId(productId);
        onboardedProduct2.setStatus(OnboardedProductState.SUSPENDED);
        onboardedProduct2.setRole(PartyRole.OPERATOR);
        onboardedProduct2.setProductRole("admin2");

        userInstitution.setProducts(List.of(onboardedProduct2, onboardedProduct));
        return userInstitution;
    }

    @Test
    void checkRoleValid() {
        when(productService.validateProductRole(any(), any(), any())).thenReturn(new ProductRole());
        when(productService.getProduct(any())).thenReturn(new Product());
        Assertions.assertDoesNotThrow(() -> userUtils.checkProductRolesAndValidateRequestMultirole("prod-pagopa", PartyRole.MANAGER, List.of("operatore")));
    }

    @Test
    void checkRoleProductRoleNotFound() {
        when(productService.validateProductRole(any(), any(), any())).thenThrow(new IllegalArgumentException("RoleMappings map for product prod-pagopa not found"));
        Assertions.assertThrows(InvalidRequestException.class, () -> userUtils.checkProductRolesAndValidateRequestMultirole("prod-pagopa", PartyRole.MANAGER, List.of("amministratore")), "RoleMappings map for product prod-pagopa not found");
    }

    @Test
    void checkRoleProductRoleListNotExists() {
        when(productService.validateProductRole(any(), any(), any())).thenThrow(new IllegalArgumentException("Role DELEGATE not found"));
        Assertions.assertThrows(InvalidRequestException.class, () -> userUtils.checkProductRolesAndValidateRequestMultirole("prod-pagopa", PartyRole.DELEGATE, List.of("operatore")), "Role DELEGATE not found");
    }

    @Test
    void checkProductRoleWithoutProductRole() {
        when(productService.validateProductRole(any(), any(), any())).thenThrow(new IllegalArgumentException("ProductRole operatore not found for role MANAGER"));
        Assertions.assertThrows(InvalidRequestException.class, () -> userUtils.checkProductRolesAndValidateRequestMultirole("prod-io", PartyRole.MANAGER, List.of("operatore")), "ProductRole operatore not found for role MANAGER");
    }

    @Test
    void checkProductRoleWithoutRole() {
        when(productService.validateProductRole(any(), any(), any())).thenThrow(new IllegalArgumentException("Role is mandatory to check productRole"));
        Assertions.assertThrows(InvalidRequestException.class, () -> userUtils.checkProductRolesAndValidateRequestMultirole("prod-io", null, List.of("operatore")), "Role is mandatory to check productRole");
    }

    @Test
    void checkRoleWithoutProduct() {
        when(productService.validateProductRole(any(), any(), any())).thenThrow(new IllegalArgumentException("ProductRole admin not found for role MANAGER"));
        Assertions.assertThrows(InvalidRequestException.class, () -> userUtils.checkProductRolesAndValidateRequestMultirole("prod-io", PartyRole.MANAGER, List.of("admin")), "ProductRole admin not found for role MANAGER");
    }

    @Test
    void testBuildWorkContact() {
        String mail = "test@example.com";
        WorkContactResource workContact = UserUtils.buildWorkContact(mail);

        assertNotNull(workContact);
        assertEquals(mail, workContact.getEmail().getValue());
        assertEquals(EmailCertifiableSchema.CertificationEnum.NONE, workContact.getEmail().getCertification());
    }

    @Test
    void testIsUserNotFoundExceptionOnUserRegistry() {
        // Prepare
        WebApplicationException webApplicationException = Mockito.mock(WebApplicationException.class);
        Response response = mock(Response.class);
        Mockito.when(webApplicationException.getResponse()).thenReturn(response);
        Mockito.when(response.getStatus()).thenReturn(HttpStatus.SC_NOT_FOUND);

        // Execute
        boolean result = UserUtils.isUserNotFoundExceptionOnUserRegistry(webApplicationException);

        // Verify
        Assertions.assertTrue(result);
    }

    @Test
    void testIsUserNotFoundExceptionOnUserRegistry_NotFoundStatus() {
        // Prepare
        WebApplicationException webApplicationException = Mockito.mock(WebApplicationException.class);
        Response response = mock(Response.class);
        Mockito.when(webApplicationException.getResponse()).thenReturn(response);
        Mockito.when(response.getStatus()).thenReturn(HttpStatus.SC_BAD_REQUEST);

        // Execute
        boolean result = UserUtils.isUserNotFoundExceptionOnUserRegistry(webApplicationException);

        // Verify
        Assertions.assertFalse(result);
    }

    @Test
    void testIsUserNotFoundExceptionOnUserRegistry_NotWebApplicationException() {
        // Prepare
        Throwable throwable = new Throwable();

        // Execute
        boolean result = UserUtils.isUserNotFoundExceptionOnUserRegistry(throwable);

        // Verify
        Assertions.assertFalse(result);
    }

    @Test
    void testGetMailUuidFromMail() {
        // Prepare test data
        String email = "test@example.com";
        Map<String, WorkContactResource> workContacts = new HashMap<>();
        WorkContactResource workContact1 = new WorkContactResource();
        workContact1.setEmail(new EmailCertifiableSchema());
        workContact1.getEmail().setValue(email);
        workContacts.put(MAIL_ID_PREFIX + "mail1", workContact1);
        WorkContactResource workContact2 = new WorkContactResource();
        workContact2.setEmail(new EmailCertifiableSchema());
        workContact2.getEmail().setValue("another@example.com");
        workContacts.put("mail2", workContact2);

        // Execute the method
        Optional<String> result = userUtils.getMailUuidFromMail(workContacts, email);

        // Verify the result
        assertTrue(result.isPresent());
        assertEquals(MAIL_ID_PREFIX + "mail1", result.get());
    }


    @Test
    void buildUsersNotificationResponseTest() {
        UUID uuid = UUID.randomUUID();
        final String userId = uuid.toString();
        String institutionId = "institutionId";
        String productId = "productId";
        UserResource userResource = getUserResource(uuid);
        UserInstitution userInstitution = getUserInstitution(userId, institutionId, productId);
        List<UserNotificationToSend> response = userUtils.buildUsersNotificationResponse(userInstitution, userResource, productId);
        Assertions.assertEquals(2, response.size());

        Assertions.assertEquals(institutionId, response.get(0).getInstitutionId());
        Assertions.assertEquals(productId, response.get(0).getProductId());
        Assertions.assertNotNull(response.get(0).getUser());

    }

    @Test
    void buildUsersNotificationResponseWithEventTest() {
        UUID uuid = UUID.randomUUID();
        final String userId = uuid.toString();
        String institutionId = "institutionId";
        String productId = "productId";
        UserResource userResource = getUserResource(uuid);
        UserInstitution userInstitution = getUserInstitution(userId, institutionId, productId);
        List<UserNotificationToSend> response = userUtils.buildUsersNotificationResponse(userInstitution, userResource);
        Assertions.assertEquals(2, response.size());
        Assertions.assertNotNull(response.get(0).getUser());
        Assertions.assertNotNull(response.get(1).getUser());

        BiFunction<UserNotificationToSend, OnboardedProductState, Boolean> checkResponseByStatus = (un, s) ->
                un.getInstitutionId().equals(institutionId) && un.getUser().getRelationshipStatus().equals(s) && un.getProductId().equals(productId);
        Assertions.assertTrue(response.stream().anyMatch(u -> checkResponseByStatus.apply(u, ACTIVE)));
        Assertions.assertTrue(response.stream().anyMatch(u -> checkResponseByStatus.apply(u, OnboardedProductState.SUSPENDED)));
    }

  @Test
  void checkProductRolesAndValidateRequestMultirole_nullProductRoles() {

    userUtils
      .checkProductRolesAndValidateRequestMultirole(
        "prod",
        PartyRole.MANAGER,
        null)
      .subscribe()
      .withSubscriber(UniAssertSubscriber.create())
      .assertCompleted();
  }

  @Test
  void checkProductRolesAndValidateRequestMultirole_singleRole() {

    ProductRole role = new ProductRole();
    role.setCode("admin");
    role.setMultiroleGroups(List.of("GROUP_A"));

    when(productService.validateProductRole(
      "prod",
      "admin",
      PartyRole.MANAGER))
      .thenReturn(role);

    when(productService.getProduct("prod")).thenReturn(new Product());

    userUtils
      .checkProductRolesAndValidateRequestMultirole(
        "prod",
        PartyRole.MANAGER,
        List.of("admin"))
      .subscribe()
      .withSubscriber(UniAssertSubscriber.create())
      .assertCompleted();
  }

  @Test
  void checkProductRolesAndValidateRequestMultirole_sameGroup() {

    ProductRole r1 = new ProductRole();
    r1.setCode("admin");
    r1.setMultiroleGroups(List.of("GROUP_A"));

    ProductRole r2 = new ProductRole();
    r2.setCode("OPERATOR");
    r2.setMultiroleGroups(List.of("GROUP_A", "GROUP_B"));

    when(productService.validateProductRole(
      "prod",
      "admin",
      PartyRole.MANAGER))
      .thenReturn(r1);

    when(productService.validateProductRole(
      "prod",
      "OPERATOR",
      PartyRole.MANAGER))
      .thenReturn(r2);

    Product product = new Product();
    ProductRoleInfo roleInfo = new ProductRoleInfo();
    roleInfo.setRoles(List.of(r1, r2));
    product.setRoleMappings(Map.of(PartyRole.MANAGER, roleInfo));

    when(productService.getProduct("prod")).thenReturn(product);

    userUtils
      .checkProductRolesAndValidateRequestMultirole(
        "prod",
        PartyRole.MANAGER,
        List.of("admin","OPERATOR"))
      .subscribe()
      .withSubscriber(UniAssertSubscriber.create())
      .assertCompleted();
  }

  @Test
  void checkProductRolesAndValidateRequestMultirole_differentGroups() {

    ProductRole r1 = new ProductRole();
    r1.setCode("viewer");
    r1.setMultiroleGroups(List.of("A"));

    ProductRole r2 = new ProductRole();
    r2.setCode("operator-api");
    r2.setMultiroleGroups(List.of("B"));

    when(productService.validateProductRole(any(), eq("viewer"), any()))
      .thenReturn(r1);

    when(productService.validateProductRole(any(), eq("operator-api"), any()))
      .thenReturn(r2);

    Product product = new Product();
    ProductRoleInfo roleInfo = new ProductRoleInfo();
    roleInfo.setRoles(List.of(r1, r2));
    product.setRoleMappings(Map.of(PartyRole.OPERATOR, roleInfo));

    when(productService.getProduct("prod")).thenReturn(product);

    InvalidRequestException ex = assertThrows(
      InvalidRequestException.class,
      () -> userUtils.checkProductRolesAndValidateRequestMultirole(
        "prod",
        PartyRole.OPERATOR,
        List.of("operator-api", "viewer"))
    );

    assertEquals(
      "Not valid multirole configuration",
      ex.getMessage());
  }

  @Test
  void checkProductRolesAndValidateRequestMultirole_singleRoleMixed() {

    ProductRole r1 = new ProductRole();
    r1.setCode("admin");
    r1.setMultiroleGroups(Collections.emptyList());

    ProductRole r2 = new ProductRole();
    r2.setCode("OPERATOR");
    r2.setMultiroleGroups(List.of("A"));

    when(productService.validateProductRole(any(), eq("admin"), any()))
      .thenReturn(r1);

    when(productService.validateProductRole(any(), eq("OPERATOR"), any()))
      .thenReturn(r2);

    Product product = new Product();
    ProductRoleInfo roleInfo = new ProductRoleInfo();
    roleInfo.setRoles(List.of(r1, r2));
    product.setRoleMappings(Map.of(PartyRole.MANAGER, roleInfo));

    when(productService.getProduct("prod")).thenReturn(product);

    InvalidRequestException ex = assertThrows(
      InvalidRequestException.class,
      () -> userUtils.checkProductRolesAndValidateRequestMultirole(
        "prod",
        PartyRole.MANAGER,
        List.of("admin", "OPERATOR"))
    );

    assertEquals(
      "Not valid multirole configuration",
      ex.getMessage());
  }
  @Test
  void checkProductRolesAndValidateRequestMultirole_invalidRole() {

    when(productService.validateProductRole(any(), any(), any()))
      .thenThrow(new IllegalArgumentException("boom"));

    InvalidRequestException ex = assertThrows(
      InvalidRequestException.class,
      () -> userUtils.checkProductRolesAndValidateRequestMultirole(
        "prod",
        PartyRole.MANAGER,
        List.of("admin"))
    );
    assertEquals("boom", ex.getMessage());
  }

  @Test
  void validateMultiroleWithUserInstitution_nullUserInstitution() {

    userUtils
      .validateMultiroleWithUserInstitution(
        "prod",
        PartyRole.MANAGER.name(),
        List.of("admin"),
        null)
      .subscribe()
      .withSubscriber(UniAssertSubscriber.create())
      .assertCompleted();

    Mockito.verifyNoInteractions(productService);
  }

  @Test
  void validateMultiroleWithUserInstitution_blankProductId() {

    userUtils
      .validateMultiroleWithUserInstitution(
        "",
        PartyRole.MANAGER.name(),
        List.of("admin"),
        new UserInstitution())
      .subscribe()
      .withSubscriber(UniAssertSubscriber.create())
      .assertCompleted();

    Mockito.verifyNoInteractions(productService);
  }

  @Test
  void validateMultiroleWithUserInstitution_nullProductRoles() {

    userUtils
      .validateMultiroleWithUserInstitution(
        "prod",
        PartyRole.MANAGER.name(),
        null,
        new UserInstitution())
      .subscribe()
      .withSubscriber(UniAssertSubscriber.create())
      .assertCompleted();

    Mockito.verifyNoInteractions(productService);
  }

  @Test
  void validateMultiroleWithUserInstitution_emptyProductRoles() {

    userUtils
      .validateMultiroleWithUserInstitution(
        "prod",
        PartyRole.MANAGER.name(),
        Collections.emptyList(),
        new UserInstitution())
      .subscribe()
      .withSubscriber(UniAssertSubscriber.create())
      .assertCompleted();

    Mockito.verifyNoInteractions(productService);
  }

  @Test
  void validateMultiroleWithUserInstitution_noExistingRoles() {

    UserInstitution ui = new UserInstitution();

    OnboardedProduct onboardedProduct = new OnboardedProduct();
    onboardedProduct.setProductId("prod");
    onboardedProduct.setRole(PartyRole.MANAGER);
    onboardedProduct.setProductRole("admin");
    onboardedProduct.setStatus(DELETED);


    ui.setProducts(List.of(
      onboardedProduct));

    userUtils
      .validateMultiroleWithUserInstitution(
        "prod",
        PartyRole.MANAGER.name(),
        List.of("admin"),
        ui)
      .subscribe()
      .withSubscriber(UniAssertSubscriber.create())
      .assertCompleted();
  }

  @Test
  void validateMultiroleWithUserInstitution_missingRoleMapping() {

    Product product = new Product();
    product.setRoleMappings(new HashMap<>());

    when(productService.getProduct("prod"))
      .thenReturn(product);

    OnboardedProduct onboardedProduct = new OnboardedProduct();
    onboardedProduct.setProductId("prod");
    onboardedProduct.setRole(PartyRole.MANAGER);
    onboardedProduct.setProductRole("admin");
    onboardedProduct.setStatus(ACTIVE);

    UserInstitution ui = new UserInstitution();
    ui.setProducts(List.of(onboardedProduct));

    InvalidRequestException ex = assertThrows(
      InvalidRequestException.class,
      () -> userUtils.validateMultiroleWithUserInstitution(
        "prod",
        PartyRole.MANAGER.name(),
        List.of("admin"),
        ui)
    );

    assertEquals(
      "No role mapping for partyRole MANAGER",
      ex.getMessage()
    );
  }

  @Test
  void validateMultiroleWithUserInstitution_existingSingleRole() {

    Product product = buildProductWithoutGroups();

    when(productService.getProduct("prod"))
      .thenReturn(product);

    UserInstitution ui = new UserInstitution();

    OnboardedProduct onboardedProduct = new OnboardedProduct();
    onboardedProduct.setProductId("prod");
    onboardedProduct.setRole(PartyRole.MANAGER);
    onboardedProduct.setProductRole("admin");
    onboardedProduct.setStatus(ACTIVE);

    ui.setProducts(List.of(onboardedProduct));

    InvalidRequestException ex = assertThrows(
      InvalidRequestException.class,
      () -> userUtils.validateMultiroleWithUserInstitution(
        "prod",
        PartyRole.MANAGER.name(),
        List.of("admin"),
        ui)
    );
    assertEquals(
      "Not valid multirole configuration",
      ex.getMessage());
  }

  @Test
  void validateMultiroleWithUserInstitution_validIntersection() {

    Product product = buildProductWithGroup("A");

    when(productService.getProduct("prod"))
      .thenReturn(product);

    UserInstitution ui = new UserInstitution();

    OnboardedProduct onboardedProduct = new OnboardedProduct();
    onboardedProduct.setProductId("prod");
    onboardedProduct.setRole(PartyRole.MANAGER);
    onboardedProduct.setProductRole("admin");
    onboardedProduct.setStatus(ACTIVE);

    ui.setProducts(List.of(
      onboardedProduct));

    userUtils
      .validateMultiroleWithUserInstitution(
        "prod",
        PartyRole.MANAGER.name(),
        List.of("admin"),
        ui)
      .subscribe()
      .withSubscriber(UniAssertSubscriber.create())
      .assertCompleted();
  }

  private Product buildProductWithGroup(String group) {

    ProductRole productRole = new ProductRole();
    productRole.setCode("admin");
    productRole.setMultiroleGroups(List.of(group));

    ProductRoleInfo roleInfo = new ProductRoleInfo();
    roleInfo.setRoles(List.of(productRole));

    Map<PartyRole, ProductRoleInfo> mappings = new HashMap<>();
    mappings.put(PartyRole.MANAGER, roleInfo);

    Product product = new Product();
    product.setRoleMappings(mappings);

    return product;
  }

  private Product buildProductWithoutGroups() {

    ProductRole productRole = new ProductRole();
    productRole.setCode("admin");
    productRole.setMultiroleGroups(Collections.emptyList());

    ProductRoleInfo roleInfo = new ProductRoleInfo();
    roleInfo.setRoles(List.of(productRole));

    Map<PartyRole, ProductRoleInfo> mappings = new HashMap<>();
    mappings.put(PartyRole.MANAGER, roleInfo);

    Product product = new Product();
    product.setRoleMappings(mappings);

    return product;
  }

  @Test
  void validateMultiroleAfterStatusUpdate_deletedStatus() {

    UserInstitution ui = new UserInstitution();

    userUtils.validateMultiroleAfterStatusUpdate(
        List.of(ui),
        "prod",
        PartyRole.MANAGER,
        "admin",
        DELETED)
      .subscribe()
      .withSubscriber(UniAssertSubscriber.create())
      .assertCompleted();

    Mockito.verifyNoInteractions(productService);
  }

  @Test
  void validateMultiroleAfterStatusUpdate_activeValid() {

    Product product = buildProductWithGroup("GROUP_A");
    when(productService.getProduct("prod")).thenReturn(product);

    OnboardedProduct p1 = new OnboardedProduct();
    p1.setProductId("prod");
    p1.setRole(PartyRole.MANAGER);
    p1.setProductRole("admin");
    p1.setStatus(OnboardedProductState.SUSPENDED);

    OnboardedProduct p2 = new OnboardedProduct();
    p2.setProductId("prod");
    p2.setRole(PartyRole.MANAGER);
    p2.setProductRole("admin");
    p2.setStatus(ACTIVE);

    UserInstitution ui = new UserInstitution();
    ui.setProducts(List.of(p1, p2));

    userUtils.validateMultiroleAfterStatusUpdate(
        List.of(ui),
        "prod",
        PartyRole.MANAGER,
        "admin",
        ACTIVE)
      .subscribe()
      .withSubscriber(UniAssertSubscriber.create())
      .assertCompleted();
  }

  @Test
  void validateMultiroleAfterStatusUpdate_invalidConfiguration() {

    Product product = buildProductWithoutGroups();
    when(productService.getProduct("prod")).thenReturn(product);

    OnboardedProduct p1 = new OnboardedProduct();
    p1.setProductId("prod");
    p1.setRole(PartyRole.MANAGER);
    p1.setProductRole("admin");
    p1.setStatus(OnboardedProductState.SUSPENDED);

    OnboardedProduct p2 = new OnboardedProduct();
    p2.setProductId("prod");
    p2.setRole(PartyRole.MANAGER);
    p2.setProductRole("admin");
    p2.setStatus(ACTIVE);

    UserInstitution ui = new UserInstitution();
    ui.setProducts(List.of(p1, p2));

    InvalidRequestException ex = assertThrows(
      InvalidRequestException.class,
      () -> userUtils.validateMultiroleAfterStatusUpdate(
        List.of(ui),
        "prod",
        PartyRole.MANAGER,
        "admin",
        ACTIVE));

    assertEquals("Not valid multirole configuration", ex.getMessage());
  }

  @Test
  void validateMultiroleAfterStatusUpdate_suspendLeavesSingleRole() {

    Product product = buildProductWithoutGroups();
    when(productService.getProduct("prod")).thenReturn(product);

    OnboardedProduct p = new OnboardedProduct();
    p.setProductId("prod");
    p.setRole(PartyRole.MANAGER);
    p.setProductRole("admin");
    p.setStatus(ACTIVE);

    UserInstitution ui = new UserInstitution();
    ui.setProducts(List.of(p));

    userUtils.validateMultiroleAfterStatusUpdate(
        List.of(ui),
        "prod",
        PartyRole.MANAGER,
        "admin",
        OnboardedProductState.SUSPENDED)
      .subscribe()
      .withSubscriber(UniAssertSubscriber.create())
      .assertCompleted();

    assertEquals(ACTIVE, p.getStatus());
  }

  @Test
  void validateMultiroleAfterStatusUpdate_multipleUserInstitutions() {

    Product product = buildProductWithGroup("A");
    when(productService.getProduct("prod")).thenReturn(product);

    OnboardedProduct p1 = new OnboardedProduct();
    p1.setProductId("prod");
    p1.setRole(PartyRole.MANAGER);
    p1.setProductRole("admin");
    p1.setStatus(ACTIVE);

    UserInstitution ui1 = new UserInstitution();
    ui1.setProducts(List.of(p1));

    OnboardedProduct p2 = new OnboardedProduct();
    p2.setProductId("prod");
    p2.setRole(PartyRole.MANAGER);
    p2.setProductRole("admin");
    p2.setStatus(OnboardedProductState.SUSPENDED);

    UserInstitution ui2 = new UserInstitution();
    ui2.setProducts(List.of(p2));

    userUtils.validateMultiroleAfterStatusUpdate(
        List.of(ui1, ui2),
        "prod",
        PartyRole.MANAGER,
        "admin",
        ACTIVE)
      .subscribe()
      .withSubscriber(UniAssertSubscriber.create())
      .assertCompleted();

    Mockito.verify(productService, Mockito.times(1)).getProduct("prod");
  }


  /**
   * Test: isExcludeRoleFromUserGroups should return true when product is null
   */
  @Test
  void isExcludeRoleFromUserGroups_shouldReturnTrue_whenProductIsNull() {
    OnboardedProduct onboardedProduct = createOnboardedProduct("test-product", PartyRole.MANAGER);

    when(productService.getProduct("test-product")).thenReturn(null);

    boolean result = userUtils.isExcludeRoleFromUserGroups(onboardedProduct);

    assertTrue(result);
  }

  /**
   * Test: isExcludeRoleFromUserGroups should return true when roleMappings is null
   */
  @Test
  void isExcludeRoleFromUserGroups_shouldReturnTrue_whenRoleMappingsIsNull() {
    OnboardedProduct onboardedProduct = createOnboardedProduct("test-product", PartyRole.MANAGER);
    Product product = new Product();
    product.setRoleMappings(null);

    when(productService.getProduct("test-product")).thenReturn(product);

    boolean result = userUtils.isExcludeRoleFromUserGroups(onboardedProduct);

    assertTrue(result);
  }

  /**
   * Test: isExcludeRoleFromUserGroups should return true when onboardedProduct role is null
   */
  @Test
  void isExcludeRoleFromUserGroups_shouldReturnTrue_whenOnboardedProductRoleIsNull() {
    OnboardedProduct onboardedProduct = createOnboardedProduct("test-product", null);
    Product product = new Product();
    product.setRoleMappings(Map.of());

    when(productService.getProduct("test-product")).thenReturn(product);

    boolean result = userUtils.isExcludeRoleFromUserGroups(onboardedProduct);

    assertTrue(result);
  }

  /**
   * Test: isExcludeRoleFromUserGroups should return true when roleInfo is null
   */
  @Test
  void isExcludeRoleFromUserGroups_shouldReturnTrue_whenRoleInfoIsNull() {
    OnboardedProduct onboardedProduct = createOnboardedProduct("test-product", PartyRole.MANAGER);
    Product product = new Product();
    product.setRoleMappings(Map.of()); // Empty mapping, so MANAGER will not be found

    when(productService.getProduct("test-product")).thenReturn(product);

    boolean result = userUtils.isExcludeRoleFromUserGroups(onboardedProduct);

    assertTrue(result);
  }

  /**
   * Test: isExcludeRoleFromUserGroups should return true when roleInfo.isExcludeRoleFromUserGroups is true
   */
  @Test
  void isExcludeRoleFromUserGroups_shouldReturnTrue_whenRoleInfoExcludeFlagIsTrue() {
    OnboardedProduct onboardedProduct = createOnboardedProduct("test-product", PartyRole.MANAGER);

    ProductRoleInfo roleInfo = new ProductRoleInfo();
    roleInfo.setExcludeRoleFromUserGroups(true);

    Product product = new Product();
    product.setRoleMappings(Map.of(PartyRole.MANAGER, roleInfo));

    when(productService.getProduct("test-product")).thenReturn(product);

    boolean result = userUtils.isExcludeRoleFromUserGroups(onboardedProduct);

    assertTrue(result);
  }

  /**
   * Test: isExcludeRoleFromUserGroups should return false when roleInfo.isExcludeRoleFromUserGroups is false
   */
  @Test
  void isExcludeRoleFromUserGroups_shouldReturnFalse_whenRoleInfoExcludeFlagIsFalse() {
    OnboardedProduct onboardedProduct = createOnboardedProduct("test-product", PartyRole.DELEGATE);

    ProductRoleInfo roleInfo = new ProductRoleInfo();
    roleInfo.setExcludeRoleFromUserGroups(false);

    Product product = new Product();
    product.setRoleMappings(Map.of(PartyRole.DELEGATE, roleInfo));

    when(productService.getProduct("test-product")).thenReturn(product);

    boolean result = userUtils.isExcludeRoleFromUserGroups(onboardedProduct);

    assertFalse(result);
  }

  /**
   * Test: isExcludeRoleFromUserGroups with multiple roles - test OPERATOR role
   */
  @Test
  void isExcludeRoleFromUserGroups_shouldReturnFalse_whenOperatorRoleExcludeFlagIsFalse() {
    OnboardedProduct onboardedProduct = createOnboardedProduct("test-product", PartyRole.OPERATOR);

    ProductRoleInfo operatorRoleInfo = new ProductRoleInfo();
    operatorRoleInfo.setExcludeRoleFromUserGroups(false);

    ProductRoleInfo managerRoleInfo = new ProductRoleInfo();
    managerRoleInfo.setExcludeRoleFromUserGroups(true);

    Product product = new Product();
    product.setRoleMappings(Map.of(
      PartyRole.OPERATOR, operatorRoleInfo,
      PartyRole.MANAGER, managerRoleInfo
    ));

    when(productService.getProduct("test-product")).thenReturn(product);

    boolean result = userUtils.isExcludeRoleFromUserGroups(onboardedProduct);

    assertFalse(result);
  }

  /**
   * Test: isExcludeRoleFromUserGroups with ADMIN_EA role
   */
  @Test
  void isExcludeRoleFromUserGroups_shouldReturnTrue_whenAdminEARoleExcludeFlagIsTrue() {
    OnboardedProduct onboardedProduct = createOnboardedProduct("test-product", PartyRole.ADMIN_EA);

    ProductRoleInfo adminEARoleInfo = new ProductRoleInfo();
    adminEARoleInfo.setExcludeRoleFromUserGroups(true);

    Product product = new Product();
    product.setRoleMappings(Map.of(PartyRole.ADMIN_EA, adminEARoleInfo));

    when(productService.getProduct("test-product")).thenReturn(product);

    boolean result = userUtils.isExcludeRoleFromUserGroups(onboardedProduct);

    assertTrue(result);
  }

  /**
   * Test: isExcludeRoleFromUserGroups with SUB_DELEGATE role
   */
  @Test
  void isExcludeRoleFromUserGroups_shouldReturnFalse_whenSubDelegateRoleExcludeFlagIsFalse() {
    OnboardedProduct onboardedProduct = createOnboardedProduct("test-prod", PartyRole.SUB_DELEGATE);

    ProductRoleInfo subDelegateRoleInfo = new ProductRoleInfo();
    subDelegateRoleInfo.setExcludeRoleFromUserGroups(false);

    Product product = new Product();
    product.setRoleMappings(Map.of(PartyRole.SUB_DELEGATE, subDelegateRoleInfo));

    when(productService.getProduct("test-prod")).thenReturn(product);

    boolean result = userUtils.isExcludeRoleFromUserGroups(onboardedProduct);

    assertFalse(result);
  }

  // Helper method
  private OnboardedProduct createOnboardedProduct(String productId, PartyRole role) {
    OnboardedProduct onboardedProduct = new OnboardedProduct();
    onboardedProduct.setProductId(productId);
    onboardedProduct.setRole(role);
    onboardedProduct.setStatus(OnboardedProductState.ACTIVE);
    return onboardedProduct;
  }


}
