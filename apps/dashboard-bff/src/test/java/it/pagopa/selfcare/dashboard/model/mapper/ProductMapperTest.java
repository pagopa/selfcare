package it.pagopa.selfcare.dashboard.model.mapper;

import it.pagopa.selfcare.commons.base.security.SelfCareAuthority;
import it.pagopa.selfcare.dashboard.model.product.*;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.product.entity.BackOfficeConfigurations;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.entity.ProductRole;
import it.pagopa.selfcare.product.entity.ProductRoleInfo;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static it.pagopa.selfcare.commons.utils.TestUtils.mockInstance;
import static org.junit.jupiter.api.Assertions.*;

class ProductMapperTest {

    @Test
    void toResource_notNull() {
        // given
        ProductTree product = new ProductTree();
        Product node = new Product();
        node.setId("Node");
        node.setBackOfficeEnvironmentConfigurations(Map.of("test", mockInstance(new BackOfficeConfigurations())));
        Product children = new Product();
        children.setId("children");
        product.setNode(node);
        product.setChildren(List.of(children));
        // when
        ProductsResource resource = ProductsMapper.toResource(product);
        // then
        assertEquals(product.getNode().getId(), resource.getId());
        assertEquals(product.getNode().getLogo(), resource.getLogo());
        assertEquals(product.getNode().getTitle(), resource.getTitle());
        assertEquals(product.getNode().getLogoBgColor(), resource.getLogoBgColor());
        assertEquals(product.getNode().getDescription(), resource.getDescription());
        assertEquals(product.getNode().getUrlPublic(), resource.getUrlPublic());
        assertEquals(product.getNode().getUrlBO(), resource.getUrlBO());
        assertEquals(product.getNode().isDelegable(), resource.isDelegable());
        assertNotNull(resource.getBackOfficeEnvironmentConfigurations());
        assertEquals(product.getChildren().get(0).getId(), resource.getChildren().get(0).getId());
        assertEquals(product.getChildren().get(0).getTitle(), resource.getChildren().get(0).getTitle());
        assertEquals(product.getChildren().get(0).getStatus(), resource.getChildren().get(0).getStatus());
        assertEquals(product.getChildren().get(0).getDepictImageUrl(), resource.getChildren().get(0).getImageUrl());
        assertEquals(product.getChildren().get(0).getLogo(), resource.getChildren().get(0).getLogo());
        assertEquals(product.getChildren().get(0).getLogoBgColor(), resource.getChildren().get(0).getLogoBgColor());
        assertEquals(product.getChildren().get(0).getDescription(), resource.getChildren().get(0).getDescription());
        assertEquals(product.getChildren().get(0).getUrlPublic(), resource.getChildren().get(0).getUrlPublic());
    }


    @Test
    void toResource_null() {
        // given
        ProductTree model = null;
        // when
        ProductsResource resource = ProductsMapper.toResource(model);
        // then
        assertNull(resource);
    }


    @Test
    void toProductRoleResource_null() {
        // given
        ProductRole input = null;
        // when
        ProductRoleMappingsResource.ProductRoleResource output = ProductsMapper.toProductRoleResource(input);
        // then
        assertNull(output);
    }


    @Test
    void toProductRoleResource_notNull() {
        // given
        ProductRole input = mockInstance(new ProductRole());
        // when
        ProductRoleMappingsResource.ProductRoleResource output = ProductsMapper.toProductRoleResource(input);
        // then
        assertNotNull(output);
        assertEquals(input.getCode(), output.getCode());
        assertEquals(input.getLabel(), output.getLabel());
        assertEquals(input.getDescription(), output.getDescription());
    }


    @Test
    void toProductRolesResource_fromEntry_null() {
        // given
        Map<PartyRole, ProductRoleInfo> input1 = null;
        Map<PartyRole, ProductRoleInfo> input2 = null;
        // when
        ProductRolesResource output = ProductsMapper.toProductRolesResource(input1, input2);
        // then
        assertNotNull(output);
        assertNull(output.getRoleMappings());
        assertNull(output.getPartnerTechRoleMappings());
    }


    @Test
    void toProductRolesResource_fromEntry_nullRoles() {
        // given
        ProductRoleInfo productRoleInfo = new ProductRoleInfo();
        productRoleInfo.setRoles(null);
        Map.Entry<PartyRole, ProductRoleInfo> input = Map.entry(PartyRole.DELEGATE, productRoleInfo);
        // when
        ProductRolesResource output = ProductsMapper.toProductRolesResource(Map.of(input.getKey(), input.getValue()), null);
        // then
        assertNotNull(output);
        assertTrue(output.getRoleMappings().stream().allMatch(resource -> resource.getProductRoles() == null || resource.getProductRoles().isEmpty()));
        assertNull(output.getPartnerTechRoleMappings());
    }


    @Test
    void toProductRolesResource_fromEntry_notNull() {
        // given
        ProductRoleInfo productRoleInfo = mockInstance(new ProductRoleInfo(), "setRoles");
        productRoleInfo.setRoles(List.of(mockInstance(new ProductRole())));
        Map.Entry<PartyRole, ProductRoleInfo> input = Map.entry(PartyRole.DELEGATE, productRoleInfo);
        // when
        ProductRolesResource output = ProductsMapper.toProductRolesResource(Map.of(input.getKey(), input.getValue()), Map.of(input.getKey(), input.getValue()));
        // then
        assertNotNull(output);
        assertFalse(output.getRoleMappings().isEmpty());
        assertFalse(output.getPartnerTechRoleMappings().isEmpty());
        ProductRoleMappingsResource resource = output.getRoleMappings().iterator().next();
        assertNotNull(resource.getProductRoles());
        assertEquals(productRoleInfo.getRoles().size(), resource.getProductRoles().size());
        assertEquals(input.getKey().name(), resource.getPartyRole());
        assertEquals(SelfCareAuthority.ADMIN, resource.getSelcRole());
        ProductRoleMappingsResource resourceTech = output.getPartnerTechRoleMappings().iterator().next();
        assertNotNull(resourceTech.getProductRoles());
        assertEquals(productRoleInfo.getRoles().size(), resourceTech.getProductRoles().size());
        assertEquals(input.getKey().name(), resourceTech.getPartyRole());
        assertEquals(SelfCareAuthority.ADMIN, resourceTech.getSelcRole());
    }

    @Test
    void toProductRolesResource_fromMap_notNull() {
        // given
        EnumMap<PartyRole, ProductRoleInfo> input = new EnumMap<>(PartyRole.class) {{
            put(PartyRole.MANAGER, mockInstance(new ProductRoleInfo(), 1));
            put(PartyRole.OPERATOR, mockInstance(new ProductRoleInfo(), 2));
        }};
        // when
        ProductRolesResource output = ProductsMapper.toProductRolesResource(input, input);
        // then
        assertNotNull(output);
        assertEquals(input.size(), output.getRoleMappings().size());
        assertEquals(input.size(), output.getPartnerTechRoleMappings().size());
    }


    @Test
    void toProductBackOfficeConfigurations_fromEntry_null() {
        // given
        final Map<String, BackOfficeConfigurations> input = null;
        // when
        final Collection<BackOfficeConfigurationsResource> output = ProductsMapper.toProductBackOfficeConfigurations(input);
        // then
        assertNull(output);
    }


    @Test
    void toProductBackOfficeConfigurations_fromEntry_notNull() {
        // given
        final Map<String, BackOfficeConfigurations> input = Map.of("test", mockInstance(new BackOfficeConfigurations()));
        // when
        final Collection<BackOfficeConfigurationsResource> output = ProductsMapper.toProductBackOfficeConfigurations(input);
        // then
        assertNotNull(output);
        assertEquals(1, output.size());
        BackOfficeConfigurationsResource resource = output.iterator().next();
        assertEquals("test", resource.getEnvironment());
        assertEquals(input.get("test").getUrl(), resource.getUrl());
    }


    @Test
    void toProductBackOfficeConfigurations_fromMap_null() {
        // given
        Map<String, BackOfficeConfigurations> input = null;
        // when
        final Collection<BackOfficeConfigurationsResource> output = ProductsMapper.toProductBackOfficeConfigurations(input);
        // then
        assertNull(output);
    }


    @Test
    void toProductBackOfficeConfigurations_fromMap_notNull() {
        // given
        final Map<String, BackOfficeConfigurations> input = Map.of("test", mockInstance(new BackOfficeConfigurations()));
        // when
        final Collection<BackOfficeConfigurationsResource> output = ProductsMapper.toProductBackOfficeConfigurations(input);
        // then
        assertNotNull(output);
        assertEquals(input.size(), output.size());
    }

}