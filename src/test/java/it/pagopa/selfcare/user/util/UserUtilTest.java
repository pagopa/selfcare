package it.pagopa.selfcare.user.util;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.entity.ProductRole;
import it.pagopa.selfcare.product.entity.ProductRoleInfo;
import it.pagopa.selfcare.product.service.ProductService;
import it.pagopa.selfcare.user.entity.UserInfo;
import it.pagopa.selfcare.user.entity.UserInstitution;
import it.pagopa.selfcare.user.entity.UserInstitutionRole;
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
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
        onboardedProduct.setStatus(OnboardedProductState.ACTIVE);
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
        Assertions.assertDoesNotThrow(() -> userUtils.checkProductRole("prod-pagopa", PartyRole.MANAGER, "operatore"));
    }

    @Test
    void checkRoleProductRoleNotFound() {
        when(productService.validateProductRole(any(), any(), any())).thenThrow(new IllegalArgumentException("RoleMappings map for product prod-pagopa not found"));
        Assertions.assertThrows(InvalidRequestException.class, () -> userUtils.checkProductRole("prod-pagopa", PartyRole.MANAGER, "amministratore"), "RoleMappings map for product prod-pagopa not found");
    }

    @Test
    void checkRoleProductRoleListNotExists() {
        when(productService.validateProductRole(any(), any(), any())).thenThrow(new IllegalArgumentException("Role DELEGATE not found"));
        Assertions.assertThrows(InvalidRequestException.class, () -> userUtils.checkProductRole("prod-pagopa", PartyRole.DELEGATE, "operatore"), "Role DELEGATE not found");
    }

    @Test
    void checkProductRoleWithoutProductRole() {
        when(productService.validateProductRole(any(), any(), any())).thenThrow(new IllegalArgumentException("ProductRole operatore not found for role MANAGER"));
        Assertions.assertThrows(InvalidRequestException.class, () -> userUtils.checkProductRole("prod-io", PartyRole.MANAGER, "operatore"), "ProductRole operatore not found for role MANAGER");
    }

    @Test
    void checkProductRoleWithoutRole() {
        when(productService.validateProductRole(any(), any(), any())).thenThrow(new IllegalArgumentException("Role is mandatory to check productRole"));
        Assertions.assertThrows(InvalidRequestException.class, () -> userUtils.checkProductRole("prod-io", null, "operatore"), "Role is mandatory to check productRole");
    }

    @Test
    void checkRoleWithoutProduct() {
        when(productService.validateProductRole(any(), any(), any())).thenThrow(new IllegalArgumentException("ProductRole admin not found for role MANAGER"));
        Assertions.assertThrows(InvalidRequestException.class, () -> userUtils.checkProductRole("prod-io", PartyRole.MANAGER, "admin"), "ProductRole admin not found for role MANAGER");
    }

    private Product getProductResource() {
        Product productResource = new Product();
        Map<PartyRole, ProductRoleInfo> map = new HashMap<>();
        ProductRoleInfo productRoleInfo = new ProductRoleInfo();
        ProductRole productRole = new ProductRole();
        productRole.setCode("operatore");
        productRoleInfo.setRoles(List.of(productRole));
        map.put(PartyRole.MANAGER, productRoleInfo);
        productResource.setRoleMappings(map);
        return productResource;
    }

    @Test
    void testFilterProductWorks() {
        OnboardedProduct onboardedProduct1 = new OnboardedProduct();
        onboardedProduct1.setProductId("test-id");
        onboardedProduct1.setStatus(OnboardedProductState.ACTIVE);
        OnboardedProduct onboardedProduct2 = new OnboardedProduct();
        onboardedProduct2.setProductId("test-id");
        onboardedProduct2.setStatus(OnboardedProductState.DELETED);

        List<OnboardedProduct> onboardedProducts = new ArrayList<>();
        onboardedProducts.add(onboardedProduct1);
        onboardedProducts.add(onboardedProduct2);

        UserInstitution userInstitution = new UserInstitution();
        userInstitution.setProducts(onboardedProducts);

        String[] states = {"ACTIVE"};
        UserInstitution filteredUserInstitution = userUtils.filterProduct(userInstitution, states);
        Assertions.assertEquals(1, filteredUserInstitution.getProducts().size());
    }

    @Test
    void testFilterInstitutionRolesWorks() {

        UserInstitutionRole userInstitution = new UserInstitutionRole();
        userInstitution.setInstitutionName("test-institutionId");
        userInstitution.setStatus(OnboardedProductState.ACTIVE);
        UserInstitutionRole userInstitution2 = new UserInstitutionRole();
        userInstitution2.setInstitutionName("test-institutionId-2");
        userInstitution2.setStatus(OnboardedProductState.PENDING);

        List<UserInstitutionRole> userInstitutionsRole = new ArrayList<>();
        userInstitutionsRole.add(userInstitution);
        userInstitutionsRole.add(userInstitution2);

        UserInfo userInfo = new UserInfo();
        userInfo.setUserId("test-user");
        userInfo.setInstitutions(userInstitutionsRole);

        String[] states = {"ACTIVE"};

        UserInfo filteredUserInfo = userUtils.filterInstitutionRoles(userInfo, states, null);
        Assertions.assertEquals(1, filteredUserInfo.getInstitutions().size());
    }

    @Test
    void testFilterInstitutionRolesWorks2() {

        UserInfo userInfo = new UserInfo();
        userInfo.setUserId("test-user");

        String[] states = {"ACTIVE"};

        UserInfo filteredUserInfo = userUtils.filterInstitutionRoles(userInfo, states, null);
        Assertions.assertNull(filteredUserInfo.getInstitutions());
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
        Assertions.assertTrue(response.stream().anyMatch(u -> checkResponseByStatus.apply(u, OnboardedProductState.ACTIVE)));
        Assertions.assertTrue(response.stream().anyMatch(u -> checkResponseByStatus.apply(u, OnboardedProductState.SUSPENDED)));
    }
}
