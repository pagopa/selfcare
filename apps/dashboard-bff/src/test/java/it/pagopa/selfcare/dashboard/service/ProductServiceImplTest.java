package it.pagopa.selfcare.dashboard.service;

import com.fasterxml.jackson.core.type.TypeReference;
import it.pagopa.selfcare.dashboard.client.IamExternalRestClient;
import it.pagopa.selfcare.iam.generated.openapi.v1.dto.ProductRolePermissions;
import it.pagopa.selfcare.iam.generated.openapi.v1.dto.ProductRolePermissionsList;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.entity.ProductRoleInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest extends BaseServiceTest {

    @InjectMocks
    private ProductServiceImpl productService;

    @Mock
    private it.pagopa.selfcare.product.service.ProductService productServiceExternal;

    @Mock
    private IamExternalRestClient iamExternalRestClient;



    @BeforeEach
    public void init() {
        super.setUp();
    }

    @Test
    void getProductRoles() throws IOException {

        String productId = "productId";
        String institutionType = "type";
        Map<PartyRole, ProductRoleInfo> productRoleMappingsMock = new HashMap<>();
        ProductRoleInfo productRoleInfo = new ProductRoleInfo();
        productRoleMappingsMock.put(PartyRole.MANAGER, productRoleInfo);
        ClassPathResource productResource = new ClassPathResource("stubs/Product.json");
        byte[] resourceStreamInstitution = Files.readAllBytes(productResource.getFile().toPath());
        Product product = objectMapper.readValue(resourceStreamInstitution, new TypeReference<>() {
        });

        when(productServiceExternal.getProduct(productId)).thenReturn(product);

        Map<PartyRole, ProductRoleInfo> result = productService.getProductRoles(productId, institutionType);
        Assertions.assertEquals(productRoleMappingsMock.get("MANAGER"), result.get("MANAGER"));
        Mockito.verify(productServiceExternal, Mockito.times(1)).getProduct(productId);
    }

    @Test
    void getProductRolesWithoutProductId() {

        Assertions.assertThrows(IllegalArgumentException.class, () -> productService.getProductRoles(null, null));
        Mockito.verifyNoInteractions(productServiceExternal);
    }

    @Test
    void getMyPermissions() {
        String userId = "userId";

        ProductRolePermissionsList response = new ProductRolePermissionsList();
        ProductRolePermissions productRolePermissions = new ProductRolePermissions();
        productRolePermissions.setProductId("ALL");
        productRolePermissions.setRole("SUPPORT");
        productRolePermissions.setGroup("support");
        productRolePermissions.setPermissions(List.of("Selc:AccessProductBackofficeAdmin"));
        response.setItems(List.of(productRolePermissions));

        when(iamExternalRestClient._getIAMProductRolePermissionsList(userId, null)).thenReturn(ResponseEntity.ok(response));
        Assertions.assertEquals(response, productService.getMyPermissions(userId));
        Mockito.verify(iamExternalRestClient, Mockito.times(1))._getIAMProductRolePermissionsList(userId, null);
    }

}
