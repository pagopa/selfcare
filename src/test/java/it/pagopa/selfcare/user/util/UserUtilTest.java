package it.pagopa.selfcare.user.util;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.entity.ProductRole;
import it.pagopa.selfcare.product.entity.ProductRoleInfo;
import it.pagopa.selfcare.product.service.ProductService;
import it.pagopa.selfcare.user.constant.OnboardedProductState;
import it.pagopa.selfcare.user.entity.OnboardedProduct;
import it.pagopa.selfcare.user.entity.UserInfo;
import it.pagopa.selfcare.user.entity.UserInstitution;
import it.pagopa.selfcare.user.entity.UserInstitutionRole;
import it.pagopa.selfcare.user.exception.InvalidRequestException;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.openapi.quarkus.user_registry_json.model.CertifiableFieldResourceOfstring;
import org.openapi.quarkus.user_registry_json.model.WorkContactResource;

import java.util.*;

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

    @Test
    void checkRoleValid(){
        when(productService.validateProductRole(any(), any(), any())).thenReturn(new ProductRole());
        Assertions.assertDoesNotThrow(() -> userUtils.checkProductRole("prod-pagopa", PartyRole.MANAGER, "operatore"));
    }

    @Test
    void checkRoleProductRoleNotFound(){
        when(productService.validateProductRole(any(), any(), any())).thenThrow(new IllegalArgumentException("RoleMappings map for product prod-pagopa not found"));
        Assertions.assertThrows(InvalidRequestException.class, () ->userUtils
                .checkProductRole("prod-pagopa", PartyRole.MANAGER, "amministratore"), "RoleMappings map for product prod-pagopa not found");
    }

    @Test
    void checkRoleProductRoleListNotExists(){
        when(productService.validateProductRole(any(), any(), any())).thenThrow(new IllegalArgumentException("Role DELEGATE not found"));
        Assertions.assertThrows(InvalidRequestException.class, () -> userUtils
                .checkProductRole("prod-pagopa", PartyRole.DELEGATE, "operatore"), "Role DELEGATE not found");
    }

    @Test
    void checkProductRoleWithoutProductRole(){
        when(productService.validateProductRole(any(), any(), any())).thenThrow(new IllegalArgumentException("ProductRole operatore not found for role MANAGER"));
        Assertions.assertThrows(InvalidRequestException.class, () -> userUtils
                .checkProductRole("prod-io", PartyRole.MANAGER, "operatore"), "ProductRole operatore not found for role MANAGER");
    }

    @Test
    void checkProductRoleWithoutRole(){
        when(productService.validateProductRole(any(), any(), any())).thenThrow(new IllegalArgumentException("Role is mandatory to check productRole"));
        Assertions.assertThrows(InvalidRequestException.class, () -> userUtils
                .checkProductRole("prod-io", null, "operatore"), "Role is mandatory to check productRole");
    }

    @Test
    void checkRoleWithoutProduct(){
        when(productService.validateProductRole(any(), any(), any())).thenThrow(new IllegalArgumentException("ProductRole admin not found for role MANAGER"));
        Assertions.assertThrows(InvalidRequestException.class, () -> userUtils
                .checkProductRole("prod-io", PartyRole.MANAGER, "admin"), "ProductRole admin not found for role MANAGER");
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

    // Start test generated with Copilot

    @Test
    void testBuildWorkContact() {
        String mail = "test@example.com";
        WorkContactResource workContact = UserUtils.buildWorkContact(mail);

        assertNotNull(workContact);
        assertEquals(mail, workContact.getEmail().getValue());
        assertEquals(CertifiableFieldResourceOfstring.CertificationEnum.NONE, workContact.getEmail().getCertification());
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
        workContact1.setEmail(new CertifiableFieldResourceOfstring());
        workContact1.getEmail().setValue(email);
        workContacts.put(MAIL_ID_PREFIX + "mail1", workContact1);
        WorkContactResource workContact2 = new WorkContactResource();
        workContact2.setEmail(new CertifiableFieldResourceOfstring());
        workContact2.getEmail().setValue("another@example.com");
        workContacts.put("mail2", workContact2);

        // Execute the method
        Optional<String> result = userUtils.getMailUuidFromMail(workContacts, email);

        // Verify the result
        assertTrue(result.isPresent());
        assertEquals(MAIL_ID_PREFIX + "mail1", result.get());
    }
}
