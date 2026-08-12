package it.pagopa.selfcare.user.event.mapper;

import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.user.event.entity.UserInstitution;
import it.pagopa.selfcare.user.event.model.TenantAwareFdUserNotificationToSend;
import it.pagopa.selfcare.user.event.model.TenantAwareUserNotificationToSend;
import it.pagopa.selfcare.user.model.NotificationUserType;
import it.pagopa.selfcare.user.model.OnboardedProduct;
import it.pagopa.selfcare.user.model.UserToNotify;
import it.pagopa.selfcare.user.model.constants.OnboardedProductState;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.openapi.quarkus.user_registry_json.model.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class NotificationMapperTest {

    @Test
    void toUserNotificationToSend_shouldCarryTenantIdFromUserInstitution() {
        UserInstitution userInstitution = tenantAwareUserInstitution("PNPG");
        OnboardedProduct onboardedProduct = new OnboardedProduct();
        onboardedProduct.setProductId("prod-pn");
        onboardedProduct.setProductRole("Admin");

        TenantAwareUserNotificationToSend result = new NotificationMapperImpl()
                .toUserNotificationToSend(userInstitution, onboardedProduct, getUserResource());

        assertEquals("PNPG", result.getTenantId());
    }

    @Test
    void toUserNotificationToSend_shouldLeaveTenantIdNullForLegacyRecords() {
        UserInstitution userInstitution = tenantAwareUserInstitution(null);
        OnboardedProduct onboardedProduct = new OnboardedProduct();
        onboardedProduct.setProductId("prod-pn");

        TenantAwareUserNotificationToSend result = new NotificationMapperImpl()
                .toUserNotificationToSend(userInstitution, onboardedProduct, getUserResource());

        assertNull(result.getTenantId());
    }

    @Test
    void toFdUserNotificationToSend_shouldCarryTenantIdFromUserInstitution() {
        UserInstitution userInstitution = tenantAwareUserInstitution("SELFCARE");
        OnboardedProduct onboardedProduct = new OnboardedProduct();
        onboardedProduct.setProductId("prod-pn");

        TenantAwareFdUserNotificationToSend result = new NotificationMapperImpl()
                .toFdUserNotificationToSend(userInstitution, onboardedProduct, getUserResource(), NotificationUserType.ACTIVE_USER);

        assertEquals("SELFCARE", result.getTenantId());
    }

    private UserInstitution tenantAwareUserInstitution(String tenantId) {
        UserInstitution userInstitution = new UserInstitution();
        userInstitution.setId(new ObjectId());
        userInstitution.setUserId("userId");
        userInstitution.setInstitutionId("institutionId");
        userInstitution.setTenantId(tenantId);
        return userInstitution;
    }

    @Test
    void mapUser_withValidData_shouldMapFieldsCorrectly() {
        UserResource userResource = getUserResource();
        OnboardedProduct onboardedProduct = new OnboardedProduct();
        onboardedProduct.setProductRole("Admin");
        onboardedProduct.setRole(PartyRole.MANAGER);
        onboardedProduct.setStatus(OnboardedProductState.ACTIVE);

        UserToNotify result = new NotificationMapperImpl().mapUser(userResource, "contactUuid", onboardedProduct);

        assertEquals(userResource.getId().toString(), result.getUserId());
        assertEquals("John", result.getName());
        assertEquals("Doe", result.getFamilyName());
        assertEquals("john.doe@example.com", result.getEmail());
        assertEquals("0000000000", result.getMobilePhone());
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
        assertNull(result.getMobilePhone());
        assertNull(result.getProductRole());
        assertNull(result.getRole());
        assertNull(result.getRelationshipStatus());
    }

    @Test
    void retrieveMailFromWorkContacts_withValidMailUuid_shouldReturnEmail() {
        UserResource userResource = getUserResource();

        String result = new NotificationMapperImpl().retrieveMailFromWorkContacts(userResource, "contactUuid");

        assertEquals("john.doe@example.com", result);
    }

    @Test
    void retrievePhoneFromWorkContacts_withValidMailUuid_shouldReturnEmail() {
        UserResource userResource = getUserResource();

        String result = new NotificationMapperImpl().retrievePhoneFromWorkContacts(userResource, "contactUuid");

        assertEquals("0000000000", result);
    }

    @Test
    void retrieveMailFromWorkContacts_withInvalidMailUuid_shouldReturnNull() {
        UserResource userResource = getUserResource();

        String result = new NotificationMapperImpl().retrieveMailFromWorkContacts(userResource, "invalidUuid");

        assertNull(result);
    }

    @Test
    void retrievePhoneFromWorkContacts_withInvalidMailUuid_shouldReturnNull() {
        UserResource userResource = getUserResource();

        String result = new NotificationMapperImpl().retrievePhoneFromWorkContacts(userResource, "invalidUuid");

        assertNull(result);
    }

    @Test
    void retrieveMailFromWorkContacts_withNullWorkContacts_shouldReturnNull() {
        UserResource userResource = new UserResource();

        String result = new NotificationMapperImpl().retrieveMailFromWorkContacts(userResource, "contactUuid");

        assertNull(result);
    }

    @Test
    void retrievePhoneFromWorkContacts_withNullPhone_shouldReturnNull() {
        UserResource userResource = new UserResource();
        userResource.setWorkContacts(Map.of("contactUuid", new WorkContactResource().email(new EmailCertifiableSchema().value("john.doe@example.com"))));

        String result = new NotificationMapperImpl().retrievePhoneFromWorkContacts(userResource, "contactUuid");

        assertNull(result);
    }

    private static UserResource getUserResource() {
        UserResource userResource = new UserResource();
        userResource.setId(UUID.randomUUID());
        userResource.setName(new NameCertifiableSchema().value("John"));
        userResource.setFamilyName(new FamilyNameCertifiableSchema().value("Doe"));
        WorkContactResource workContactResource = new WorkContactResource();
        workContactResource.setEmail(new EmailCertifiableSchema().value("john.doe@example.com"));
        workContactResource.setMobilePhone(new MobilePhoneCertifiableSchema().value("0000000000"));
        userResource.setWorkContacts(Map.of("contactUuid", workContactResource));
        return userResource;
    }
}
