package it.pagopa.selfcare.user.util;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.entity.ProductRole;
import it.pagopa.selfcare.product.entity.ProductRoleInfo;
import it.pagopa.selfcare.product.service.ProductService;
import it.pagopa.selfcare.user.controller.response.UserInfoResponse;
import it.pagopa.selfcare.user.controller.response.UserInstitutionRoleResponse;
import it.pagopa.selfcare.user.entity.UserInstitution;
import it.pagopa.selfcare.user.constant.OnboardedProductState;
import it.pagopa.selfcare.user.entity.OnboardedProduct;
import it.pagopa.selfcare.user.exception.InvalidRequestException;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
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

        UserInstitutionRoleResponse userInstitution = new UserInstitutionRoleResponse();
        userInstitution.setInstitutionName("test-institutionId");
        userInstitution.setStatus(OnboardedProductState.ACTIVE);
        UserInstitutionRoleResponse userInstitution2 = new UserInstitutionRoleResponse();
        userInstitution2.setInstitutionName("test-institutionId-2");
        userInstitution2.setStatus(OnboardedProductState.PENDING);

        List<UserInstitutionRoleResponse> userInstitutionsRole = new ArrayList<>();
        userInstitutionsRole.add(userInstitution);
        userInstitutionsRole.add(userInstitution2);

        UserInfoResponse userInfoResponse = new UserInfoResponse();
        userInfoResponse.setUserId("test-user");
        userInfoResponse.setInstitutions(userInstitutionsRole);

        String[] states = {"ACTIVE"};

        UserInfoResponse filteredUserInfo = userUtils.filterInstitutionRoles(userInfoResponse, states, null);
        Assertions.assertEquals(1, filteredUserInfo.getInstitutions().size());
    }

}
