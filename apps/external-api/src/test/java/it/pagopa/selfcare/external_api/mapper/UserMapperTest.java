package it.pagopa.selfcare.external_api.mapper;

import it.pagopa.selfcare.external_api.model.onboarding.User;
import it.pagopa.selfcare.external_api.model.user.*;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static it.pagopa.selfcare.commons.utils.TestUtils.mockInstance;
import static it.pagopa.selfcare.commons.utils.TestUtils.reflectionEqualsByName;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;

class UserMapperTest {

    @Test
    void toUserResource_null() {
        // given
        UserInfo model = null;
        final String productId = null;
        // when
        UserResource resource = UserMapperCustom.toUserResource(model, productId);
        // then
        assertNull(resource);
    }


    @Test
    void toUserResource_notNull() {
        // given
        UserInfo model = mockInstance(new UserInfo());
        model.setId(randomUUID().toString());
        ProductInfoDetails productMock = mockInstance(new ProductInfoDetails());
        productMock.setRoleInfos(List.of(mockInstance(new RoleInfo())));
        Map<String, ProductInfoDetails> product = new HashMap<>();
        product.put(productMock.getId(), productMock);
        model.setProducts(product);
        model.getUser().setWorkContacts(Map.of(model.getUserUuidMail(), mockInstance(new WorkContact())));
        String id = model.getProducts().keySet().toArray()[0].toString();
        ProductInfoDetails productInfo = model.getProducts().get(id);
        // when
        UserResource resource = UserMapperCustom.toUserResource(model, productMock.getId());
        // then
        assertEquals(model.getId(), resource.getId().toString());
        assertEquals(model.getUser().getName().getValue(), resource.getName());
        assertEquals(model.getUser().getFamilyName().getValue(), resource.getSurname());
        assertEquals(model.getUser().getWorkContacts().get(model.getUserUuidMail()).getEmail().getValue(), resource.getEmail());
        assertEquals(model.getPartyRole(), resource.getRole());
        assertIterableEquals(productInfo.getRoleInfos().stream().map(RoleInfo::getRole).collect(Collectors.toList()), resource.getRoles());
    }

    @Test
    void toUser() {
        // given
        UserDto model = mockInstance(new UserDto());
        // when
        User resource = UserMapperCustom.toUser(model);
        // then
        assertNotNull(resource);
        reflectionEqualsByName(model, resource);
    }

    @Test
    void toUser_null() {
        // given
        UserDto model = null;
        // when
        User resource = UserMapperCustom.toUser(model);
        // then
        assertNull(resource);
    }

}
