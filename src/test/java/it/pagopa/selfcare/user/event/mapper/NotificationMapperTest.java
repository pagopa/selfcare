package it.pagopa.selfcare.user.event.mapper;

import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.user.model.OnboardedProduct;
import it.pagopa.selfcare.user.model.UserToNotify;
import it.pagopa.selfcare.user.model.constants.OnboardedProductState;
import org.junit.jupiter.api.Test;
import org.openapi.quarkus.user_registry_json.model.CertifiableFieldResourceOfstring;
import org.openapi.quarkus.user_registry_json.model.UserResource;
import org.openapi.quarkus.user_registry_json.model.WorkContactResource;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

class NotificationMapperTest {

    @Test
    void mapUser_withValidData_shouldMapFieldsCorrectly() {
        UserResource userResource = new UserResource();
        userResource.setId(UUID.randomUUID());
        userResource.setName(new CertifiableFieldResourceOfstring().value("John"));
        userResource.setFamilyName(new CertifiableFieldResourceOfstring().value("Doe"));
        userResource.setWorkContacts(Map.of("mailUuid", new WorkContactResource().email(new CertifiableFieldResourceOfstring().value("john.doe@example.com"))));
        OnboardedProduct onboardedProduct = new OnboardedProduct();
        onboardedProduct.setProductRole("Admin");
        onboardedProduct.setRole(PartyRole.MANAGER);
        onboardedProduct.setStatus(OnboardedProductState.ACTIVE);

        UserToNotify result = new NotificationMapperImpl().mapUser(userResource, "mailUuid", onboardedProduct);

        assertEquals(userResource.getId().toString(), result.getUserId());
        assertEquals("John", result.getName());
        assertEquals("Doe", result.getFamilyName());
        assertEquals("john.doe@example.com", result.getEmail());
        assertEquals("Admin", result.getProductRole());
        assertEquals("MANAGER", result.getRole());
        assertEquals(OnboardedProductState.ACTIVE, result.getRelationshipStatus());
    }

    @Test
    void mapUserForFdTest() {
        UserResource userResource = new UserResource();
        userResource.setId(UUID.randomUUID());
        OnboardedProduct onboardedProduct = new OnboardedProduct();
        onboardedProduct.setProductRole("Admin");
        onboardedProduct.setRole(PartyRole.MANAGER);

        UserToNotify result = new NotificationMapperImpl().mapUserForFD(userResource,  onboardedProduct);

        assertEquals(userResource.getId().toString(), result.getUserId());
        assertEquals(List.of("Admin"), result.getRoles());
        assertEquals("MANAGER", result.getRole());
    }

    @Test
    void mapUser_withNullFields_shouldHandleNullValues() {
        UserResource userResource = new UserResource();
        OnboardedProduct onboardedProduct = new OnboardedProduct();

        UserToNotify result = new NotificationMapperImpl().mapUser(userResource, null, onboardedProduct);

        assertNull(result.getUserId());
        assertNull(result.getName());
        assertNull(result.getFamilyName());
        assertNull(result.getEmail());
        assertNull(result.getProductRole());
        assertNull(result.getRole());
        assertNull(result.getRelationshipStatus());
    }

    @Test
    void retrieveMailFromWorkContacts_withValidMailUuid_shouldReturnEmail() {
        UserResource userResource = new UserResource();
        userResource.setWorkContacts(Map.of("mailUuid", new WorkContactResource().email(new CertifiableFieldResourceOfstring().value("john.doe@example.com"))));

        String result = new NotificationMapperImpl().retrieveMailFromWorkContacts(userResource, "mailUuid");

        assertEquals("john.doe@example.com", result);
    }

    @Test
    void retrieveMailFromWorkContacts_withInvalidMailUuid_shouldReturnNull() {
        UserResource userResource = new UserResource();
        userResource.setWorkContacts(Map.of("mailUuid", new WorkContactResource().email(new CertifiableFieldResourceOfstring().value("john.doe@example.com"))));

        String result = new NotificationMapperImpl().retrieveMailFromWorkContacts(userResource, "invalidUuid");

        assertNull(result);
    }

    @Test
    void retrieveMailFromWorkContacts_withNullWorkContacts_shouldReturnNull() {
        UserResource userResource = new UserResource();

        String result = new NotificationMapperImpl().retrieveMailFromWorkContacts(userResource, "mailUuid");

        assertNull(result);
    }
}
