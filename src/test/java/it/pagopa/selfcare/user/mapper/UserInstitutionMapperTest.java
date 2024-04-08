package it.pagopa.selfcare.user.mapper;

import it.pagopa.selfcare.onboarding.common.Env;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.user.constant.OnboardedProductState;
import it.pagopa.selfcare.user.controller.request.AddUserRoleDto;
import it.pagopa.selfcare.user.controller.request.CreateUserDto;
import it.pagopa.selfcare.user.entity.OnboardedProduct;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UserInstitutionMapperTest {

    private final UserInstitutionMapper mapper = new UserInstitutionMapperImpl();

    @Test
    void toNewOnboardedProduct_returnsEmptyList_whenProductRolesIsEmpty() {
        CreateUserDto.Product product = new CreateUserDto.Product();
        product.setProductRoles(new ArrayList<>());

        List<OnboardedProduct> result = mapper.toNewOnboardedProduct(product);

        assertTrue(result.isEmpty());
    }

    @Test
    void toNewOnboardedProduct_returnsOnboardedProducts_whenProductRolesIsNotEmpty() {
        CreateUserDto.Product product = new CreateUserDto.Product();
        product.setProductRoles(Arrays.asList("role1", "role2"));
        product.setProductId("productId");
        product.setTokenId("tokenId");
        product.setRole(PartyRole.MANAGER);

        List<OnboardedProduct> result = mapper.toNewOnboardedProduct(product);

        assertEquals(2, result.size());
        assertEquals("productId", result.get(0).getProductId());
        assertEquals("tokenId", result.get(0).getTokenId());
        assertEquals("role1", result.get(0).getProductRole());
        assertEquals(PartyRole.MANAGER, result.get(0).getRole());
        assertEquals(OnboardedProductState.ACTIVE, result.get(0).getStatus());
        assertEquals(Env.ROOT, result.get(0).getEnv());
    }


    @Test
    void toNewOnboardedProductFromAddUserRole_returnsOnboardedProducts_whenProductRolesIsNotEmpty() {
        AddUserRoleDto.Product product = new AddUserRoleDto.Product();
        product.setProductRoles(Arrays.asList("role1", "role2"));
        product.setProductId("productId");
        product.setTokenId("tokenId");
        product.setRole(PartyRole.MANAGER);

        List<OnboardedProduct> result = mapper.toNewOnboardedProductFromAddUserRole(product);

        assertEquals(2, result.size());
        assertEquals("productId", result.get(0).getProductId());
        assertEquals("tokenId", result.get(0).getTokenId());
        assertEquals("role1", result.get(0).getProductRole());
        assertEquals(PartyRole.MANAGER, result.get(0).getRole());
        assertEquals(OnboardedProductState.ACTIVE, result.get(0).getStatus());
        assertEquals(Env.ROOT, result.get(0).getEnv());
    }
}