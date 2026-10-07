package it.pagopa.selfcare.onboarding.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.pagopa.selfcare.onboarding.client.model.Certification;
import it.pagopa.selfcare.onboarding.client.model.CertifiedField;
import it.pagopa.selfcare.onboarding.client.model.MutableUserFieldsDto;
import it.pagopa.selfcare.onboarding.client.model.RegistryUser;
import it.pagopa.selfcare.onboarding.client.model.SaveUserDto;
import it.pagopa.selfcare.onboarding.client.model.User;
import it.pagopa.selfcare.onboarding.client.model.UserInfo;
import it.pagopa.selfcare.onboarding.client.model.WorkContact;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.onboarding.controller.request.UserDataValidationDto;
import it.pagopa.selfcare.onboarding.controller.request.UserDto;
import it.pagopa.selfcare.onboarding.controller.request.UserTaxCodeDto;
import it.pagopa.selfcare.onboarding.controller.response.ManagerInfoResponse;
import it.pagopa.selfcare.onboarding.controller.response.UserResource;
import java.util.Map;
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
        dto.setRole(PartyRole.MANAGER);
        dto.setEmail("mario.rossi@example.com");
        dto.setProductRole("admin");

        User user = userMapper.toUser(dto);

        assertEquals("Mario", user.getName());
        assertEquals("Rossi", user.getSurname());
        assertEquals("RSSMRA80A01H501U", user.getTaxCode());
        assertEquals(PartyRole.MANAGER, user.getRole());
        assertEquals("mario.rossi@example.com", user.getEmail());
        assertEquals("admin", user.getProductRole());
    }

    @Test
    void toUser_fromUserDataValidationDto() {
        UserDataValidationDto dto = new UserDataValidationDto();
        dto.setName("Mario");
        dto.setSurname("Rossi");
        dto.setTaxCode("RSSMRA80A01H501U");

        User user = userMapper.toUser(dto);

        assertEquals("Mario", user.getName());
        assertEquals("Rossi", user.getSurname());
        assertEquals("RSSMRA80A01H501U", user.getTaxCode());
    }

    @Test
    void toUser_nullInputsAreNull() {
        assertNull(userMapper.toUser((UserDto) null));
        assertNull(userMapper.toUser((UserDataValidationDto) null));
    }

    @Test
    void toManagerInfoResponse_copiesNameAndSurname() {
        User user = new User();
        user.setName("Mario");
        user.setSurname("Rossi");

        ManagerInfoResponse response = userMapper.toManagerInfoResponse(user);

        assertEquals("Mario", response.getName());
        assertEquals("Rossi", response.getSurname());
    }

    @Test
    void toString_unwrapsTheTaxCode() {
        UserTaxCodeDto dto = new UserTaxCodeDto();
        dto.setTaxCode("TAX");

        assertEquals("TAX", userMapper.toString(dto));
        assertNull(userMapper.toString((UserTaxCodeDto) null));
    }

    @Test
    void toResource_mapsTheRelationshipAndTheRegistryUser() {
        String institutionId = UUID.randomUUID().toString();
        RegistryUser registryUser = new RegistryUser();
        registryUser.setFiscalCode("RSSMRA80A01H501U");
        registryUser.setName(certified("Mario"));
        registryUser.setFamilyName(certified("Rossi"));
        WorkContact contact = new WorkContact();
        contact.setEmail(certified("mario.rossi@example.com"));
        registryUser.setWorkContacts(Map.of(institutionId, contact, "other", new WorkContact()));
        UserInfo model = new UserInfo();
        model.setId(UUID.randomUUID().toString());
        model.setInstitutionId(institutionId);
        model.setRole(PartyRole.MANAGER);
        model.setStatus("ACTIVE");
        model.setUser(registryUser);

        UserResource resource = userMapper.toResource(model);

        assertEquals(model.getId(), resource.getId().toString());
        assertEquals(institutionId, resource.getInstitutionId().toString());
        assertEquals(PartyRole.MANAGER, resource.getRole());
        assertEquals("ACTIVE", resource.getStatus());
        assertEquals("Mario", resource.getName());
        assertEquals("Rossi", resource.getSurname());
        assertEquals("RSSMRA80A01H501U", resource.getTaxCode());
        assertEquals("mario.rossi@example.com", resource.getEmail());
    }

    @Test
    void toResource_withoutRegistryUserLeavesTheUserFieldsEmpty() {
        UserInfo model = new UserInfo();
        model.setId(UUID.randomUUID().toString());
        model.setInstitutionId(UUID.randomUUID().toString());

        UserResource resource = userMapper.toResource(model);

        assertNotNull(resource);
        assertNull(resource.getName());
        assertNull(resource.getEmail());
    }

    @Test
    void toResource_nullUserInfoIsNull() {
        assertNull(userMapper.toResource(null));
    }

    @Test
    void toSaveUserDto_certifiesNothingAndKeysTheContactByInstitution() {
        User user = new User();
        user.setName("Mario");
        user.setSurname("Rossi");
        user.setTaxCode("RSSMRA80A01H501U");
        user.setEmail("mario.rossi@example.com");

        SaveUserDto dto = UserMapper.toSaveUserDto(user, "inst1");

        assertEquals("RSSMRA80A01H501U", dto.getFiscalCode());
        assertEquals("Mario", dto.getName().getValue());
        assertEquals(Certification.NONE, dto.getName().getCertification());
        assertEquals("Rossi", dto.getFamilyName().getValue());
        assertEquals("mario.rossi@example.com", dto.getWorkContacts().get("inst1").getEmail().getValue());
    }

    @Test
    void toMutableUserFieldsDto_withoutInstitutionHasNoWorkContacts() {
        User user = new User();
        user.setName("Mario");

        MutableUserFieldsDto dto = UserMapper.toMutableUserFieldsDto(user, null);

        assertEquals("Mario", dto.getName().getValue());
        assertNull(dto.getFamilyName());
        assertNull(dto.getWorkContacts());
    }

    @Test
    void nullUsersAreNotMapped() {
        assertNull(UserMapper.toSaveUserDto(null, "inst1"));
        assertNull(UserMapper.toMutableUserFieldsDto(null, "inst1"));
        assertTrue(CertifiedFieldMapper.map(null) == null);
    }

    private static CertifiedField<String> certified(String value) {
        CertifiedField<String> field = new CertifiedField<>();
        field.setValue(value);
        field.setCertification(Certification.SPID);
        return field;
    }
}
