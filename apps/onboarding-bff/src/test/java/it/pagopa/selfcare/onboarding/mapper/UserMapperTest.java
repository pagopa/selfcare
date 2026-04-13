package it.pagopa.selfcare.onboarding.mapper;

import static org.junit.jupiter.api.Assertions.*;

import it.pagopa.selfcare.commons.base.security.PartyRole;
import it.pagopa.selfcare.onboarding.client.model.CertifiedField;
import it.pagopa.selfcare.onboarding.client.model.User;
import it.pagopa.selfcare.onboarding.client.model.UserInfo;
import it.pagopa.selfcare.onboarding.controller.request.UserDataValidationDto;
import it.pagopa.selfcare.onboarding.controller.request.UserDto;
import it.pagopa.selfcare.onboarding.controller.response.UserResource;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserMapperTest {

    private final UserMapper userMapper = new UserMapperImpl();

    @Test
    void toUser_fromUserDto() {
        UserDto dto = new UserDto();
        dto.setName("Mario");
        dto.setSurname("Rossi");
        dto.setTaxCode("RSSMRA80A01H501U");
        dto.setRole(it.pagopa.selfcare.onboarding.common.PartyRole.MANAGER);
        dto.setEmail("mario.rossi@example.com");

        User resource = userMapper.toUser(dto);

        assertNotNull(resource);
        assertEquals("Mario", resource.getName().getValue());
        assertEquals("Rossi", resource.getSurname());
        assertNull(resource.getFamilyName());
        assertEquals(dto.getTaxCode(), resource.getTaxCode());
    }

    @Test
    void toUser_fromUserDataValidationDto() {
        UserDataValidationDto dto = new UserDataValidationDto();
        dto.setName("Mario");
        dto.setSurname("Rossi");
        dto.setTaxCode("RSSMRA80A01H501U");

        User resource = userMapper.toUser(dto);

        assertNotNull(resource);
        assertEquals("Mario", resource.getName().getValue());
        assertEquals("Rossi", resource.getSurname());
        assertNull(resource.getFamilyName());
        assertEquals(dto.getTaxCode(), resource.getTaxCode());
    }

    @Test
    void toResource_userResource() {
        UserInfo model = new UserInfo();
        model.setId(UUID.randomUUID().toString());
        model.setInstitutionId(UUID.randomUUID().toString());
        model.setRole(PartyRole.MANAGER);
        model.setStatus("ACTIVE");

        UserResource resource = userMapper.toResource(model);

        assertNotNull(resource);
        assertEquals(model.getId(), resource.getId().toString());
        assertEquals(model.getInstitutionId(), resource.getInstitutionId().toString());
        assertEquals(model.getRole(), resource.getRole());
        assertEquals(model.getStatus(), resource.getStatus());
    }

    @Test
    void toResource_nullUserResource() {
        assertNull(userMapper.toResource(null));
    }

    @Test
    void toMutableUserFieldsDto_mapsCertifiedFields() {
        User user = new User();
        user.setName(UserMapper.map("Mario"));
        user.setFamilyName(UserMapper.map("Rossi"));
        CertifiedField<String> email = new CertifiedField<>();
        email.setValue("mario.rossi@example.com");
        it.pagopa.selfcare.onboarding.client.model.WorkContact wc = new it.pagopa.selfcare.onboarding.client.model.WorkContact();
        wc.setEmail(email);
        user.setWorkContacts(java.util.Map.of("inst1", wc));

        var dto = UserMapper.toMutableUserFieldsDto(user, "inst1");

        assertNotNull(dto);
        assertEquals("Mario", dto.getName().getValue());
        assertEquals("Rossi", dto.getFamilyName().getValue());
        assertEquals("mario.rossi@example.com", dto.getWorkContacts().get("inst1").getEmail().getValue());
    }
}
