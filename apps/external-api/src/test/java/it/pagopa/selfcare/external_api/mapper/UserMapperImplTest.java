package it.pagopa.selfcare.external_api.mapper;

import it.pagopa.selfcare.external_api.exception.ResourceNotFoundException;
import it.pagopa.selfcare.external_api.model.user.OnboardedProductResponse;
import it.pagopa.selfcare.external_api.service.ProductMsService;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.BackOfficeRole;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.ProductResponse;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.RoleMapping;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.InstitutionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserMapperImplTest {

    private final ProductMsService productMsService = mock(ProductMsService.class);
    private final UserMapperImpl mapper = new UserMapperImpl();

    @BeforeEach
    void injectProductService() {
        ReflectionTestUtils.setField(mapper, "productMsService", productMsService);
    }

    @Test
    void prefersProductLabelAndFallsBackToRoleLabel() {
        when(productMsService.getProductRaw("with-product-label"))
                .thenReturn(product("Product administrator"));
        when(productMsService.getProductRaw("without-product-label"))
                .thenReturn(product(null));

        assertEquals("Product administrator", map("with-product-label", "MANAGER", "admin").getProductRoleLabel());
        assertEquals("Administrator", map("without-product-label", "MANAGER", "admin").getProductRoleLabel());
    }

    @Test
    void returnsNotApplicableForUnknownRoleOrProductRole() {
        when(productMsService.getProductRaw("product")).thenReturn(product(null));

        assertEquals("N.A.", map("product", "unknown-role", "admin").getProductRoleLabel());
        assertEquals("N.A.", map("product", "MANAGER", "missing").getProductRoleLabel());
    }

    @Test
    void propagatesProductNotFoundFromProductService() {
        when(productMsService.getProductRaw("missing-product"))
                .thenThrow(new ResourceNotFoundException("missing product"));

        assertThrows(ResourceNotFoundException.class,
                () -> map("missing-product", "MANAGER", "admin"));
    }

    private OnboardedProductResponse map(String productId, String role, String productRole) {
        it.pagopa.selfcare.user.generated.openapi.v1.dto.OnboardedProductResponse source =
                new it.pagopa.selfcare.user.generated.openapi.v1.dto.OnboardedProductResponse()
                .productId(productId)
                .role(role)
                .productRole(productRole);
        return mapper.onboardedProductResponseToOnboardedProductResponse(source);
    }

    private static ProductResponse product(String productLabel) {
        BackOfficeRole backOfficeRole = new BackOfficeRole();
        backOfficeRole.setCode("admin");
        backOfficeRole.setProductLabel(productLabel);
        backOfficeRole.setLabel("Administrator");
        RoleMapping roleMapping = new RoleMapping();
        roleMapping.setRole("MANAGER");
        roleMapping.setInstitutionType(InstitutionType.DEFAULT);
        roleMapping.setBackOfficeRoles(List.of(backOfficeRole));
        ProductResponse product = new ProductResponse();
        product.setRoleMappings(List.of(roleMapping));
        return product;
    }
}



