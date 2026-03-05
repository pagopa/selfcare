package it.pagopa.selfcare.product.utils;

import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.product.entity.PHASE_ADDITION_ALLOWED;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.entity.ProductRole;
import it.pagopa.selfcare.product.entity.ProductRoleInfo;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static it.pagopa.selfcare.product.utils.ProductUtils.getProductRole;
import static org.junit.jupiter.api.Assertions.*;

public class ProductUtilsTest {

    private final static String productRoleManager = "admin";

    @Test
    void validRoles() {
        Product product = dummyProduct();
        List<PartyRole> partyRoles = ProductUtils.validRoles(product, PHASE_ADDITION_ALLOWED.ONBOARDING, InstitutionType.PA);
        assertEquals(1, partyRoles.size());
        assertEquals(PartyRole.MANAGER, partyRoles.get(0));
    }

    @Test
    void validRolesByProductRole() {
        Product product = dummyProduct();
        List<PartyRole> partyRoles = ProductUtils.validRolesByProductRole(product, PHASE_ADDITION_ALLOWED.ONBOARDING, productRoleManager, InstitutionType.PA);
        assertEquals(1, partyRoles.size());
        assertEquals(PartyRole.MANAGER, partyRoles.get(0));
    }

    @Test
    void validRolesByInstitutionType() {
        Product product = dummyProduct();
        List<PartyRole> partyRoles = ProductUtils.validRoles(product, PHASE_ADDITION_ALLOWED.ONBOARDING, InstitutionType.PSP);
        assertEquals(1, partyRoles.size());
        assertEquals(PartyRole.DELEGATE, partyRoles.get(0));
    }

    @Test
    void validRolesByProductRoleByInstitutionType() {
        Product product = dummyProduct();
        List<PartyRole> partyRoles = ProductUtils.validRolesByProductRole(product, PHASE_ADDITION_ALLOWED.ONBOARDING, productRoleManager, InstitutionType.PSP);
        assertEquals(1, partyRoles.size());
        assertEquals(PartyRole.DELEGATE, partyRoles.get(0));
    }
    @Test
    public void testGetProductRole_Success() {
        // Arrange
        Product product = dummyProduct();
        PartyRole partyRole = PartyRole.MANAGER;

        // Act
        ProductRole result = getProductRole(productRoleManager, partyRole, product);

        // Assert
        assertNotNull(result);
        assertEquals(productRoleManager, result.getCode());
    }

    @Test()
    public void testGetProductRole_RoleNotFound() {
        // Arrange
        Product product = dummyProduct();
        PartyRole nonExistingPartyRole = PartyRole.DELEGATE; // DELEGATE not exists on roleMappings

        assertThrows(IllegalArgumentException.class, () -> getProductRole("someRoleCode", nonExistingPartyRole, product));
    }

    @Test()
    public void testGetProductRole_ProductRoleNotFound() {
        // Arrange
        Product product = dummyProduct();
        PartyRole partyRole = PartyRole.MANAGER;
        String nonExistingProductRoleCode = "nonExistingRole";

        // Act
        assertThrows(IllegalArgumentException.class, () -> getProductRole(nonExistingProductRoleCode, partyRole, product));
    }

    Product dummyProduct() {

        ProductRole productRole = new ProductRole();
        productRole.setCode(productRoleManager);
        ProductRoleInfo productRoleInfo = new ProductRoleInfo();
        productRoleInfo.setRoles(List.of(productRole));
        productRoleInfo.setPhasesAdditionAllowed(List.of(PHASE_ADDITION_ALLOWED.ONBOARDING.value));

        ProductRole productRoleOperator = new ProductRole();
        productRoleOperator.setCode("operator");
        ProductRoleInfo productRoleInfoOperator = new ProductRoleInfo();
        productRoleInfoOperator.setRoles(List.of(productRoleOperator));
        productRoleInfoOperator.setPhasesAdditionAllowed(List.of(PHASE_ADDITION_ALLOWED.DASHBOARD.value));

        Map<PartyRole, ProductRoleInfo> roleMapping = new HashMap<>();
        roleMapping.put(PartyRole.MANAGER, productRoleInfo);
        roleMapping.put(PartyRole.OPERATOR, productRoleInfoOperator);

        Map<String, Map<PartyRole, ProductRoleInfo>> roleMappingsByInstitutionType = new HashMap<>();
        Map<PartyRole, ProductRoleInfo> roleMappingByInstitutionType = new HashMap<>();
        roleMappingByInstitutionType.put(PartyRole.DELEGATE, productRoleInfo);
        roleMappingByInstitutionType.put(PartyRole.SUB_DELEGATE, productRoleInfoOperator);
        roleMappingsByInstitutionType.put(InstitutionType.PSP.name(), roleMappingByInstitutionType);

        Product productResource = new Product();
        productResource.setId("productId");
        productResource.setRoleMappings(roleMapping);
        productResource.setRoleMappingsByInstitutionType(roleMappingsByInstitutionType);
        return productResource;
    }
}
