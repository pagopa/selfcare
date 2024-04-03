package it.pagopa.selfcare.user.mapper;

import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.user.controller.response.CertifiableFieldResponse;
import it.pagopa.selfcare.user.controller.response.WorkContactResponse;
import org.junit.jupiter.api.Test;
import org.openapi.quarkus.user_registry_json.model.CertifiableFieldResourceOfstring;
import org.openapi.quarkus.user_registry_json.model.UserResource;
import org.openapi.quarkus.user_registry_json.model.WorkContactResource;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class UserMapperTest {

    private UserMapper userMapper = new UserMapperImpl();

    @Test
    void retrieveCertifiedEmailFromWorkContacts(){
        final UserResource userResource = new UserResource();
        final CertifiableFieldResourceOfstring email = new CertifiableFieldResourceOfstring(CertifiableFieldResourceOfstring.CertificationEnum.NONE, "email");
        WorkContactResource workContactResource = new WorkContactResource(email);
        Map<String, WorkContactResource> workContactResourceMap = Map.of("email", workContactResource);
        userResource.setWorkContacts(workContactResourceMap);

        CertifiableFieldResponse<String> certifiedMail = userMapper.retrieveCertifiedMailFromWorkContacts(userResource, "email");

        assertEquals(CertifiableFieldResourceOfstring.CertificationEnum.NONE,certifiedMail.getCertified());
        assertEquals("email", certifiedMail.getValue());
    }

    @Test
    void retrieveCertifiedEmailFromWorkContacts_nullWorkContacts(){
        final UserResource userResource = new UserResource();

        CertifiableFieldResponse<String> certifiedMail = userMapper.retrieveCertifiedMailFromWorkContacts(userResource, "email");

        assertNull(certifiedMail);
    }

    @Test
    void retrieveCertifiedEmailFromWorkContacts_notPresent(){
        final UserResource userResource = new UserResource();
        final CertifiableFieldResourceOfstring email = new CertifiableFieldResourceOfstring(CertifiableFieldResourceOfstring.CertificationEnum.NONE, "email");
        WorkContactResource workContactResource = new WorkContactResource(email);
        Map<String, WorkContactResource> workContactResourceMap = Map.of("email", workContactResource);
        userResource.setWorkContacts(workContactResourceMap);

        CertifiableFieldResponse<String> certifiedMail = userMapper.retrieveCertifiedMailFromWorkContacts(userResource, "notPresent");
        assertNull(certifiedMail);

    }


    @Test
    void retrieveMailFromWorkContacts(){
        final CertifiableFieldResourceOfstring email = new CertifiableFieldResourceOfstring(CertifiableFieldResourceOfstring.CertificationEnum.NONE, "email");
        WorkContactResource workContactResource = new WorkContactResource(email);
        Map<String, WorkContactResource> workContactResourceMap = Map.of("email", workContactResource);

        String mailFromWorkContact = userMapper.retrieveMailFromWorkContacts(workContactResourceMap, "email");

        assertEquals("email", mailFromWorkContact);
    }

    @Test
    void retrieveMailFromWorkContacts_emptyMap(){
        Map<String, WorkContactResource> workContactResourceMap = new HashMap<>();

        String mailFromWorkContact = userMapper.retrieveMailFromWorkContacts(workContactResourceMap, "email");

        assertNull(mailFromWorkContact);
    }

    @Test
    void retrieveMailFromWorkContacts_nullMap(){
        Map<String, WorkContactResource> workContactResourceMap = null;

        String mailFromWorkContact = userMapper.retrieveMailFromWorkContacts(workContactResourceMap, "email");

        assertNull(mailFromWorkContact);
    }

    @Test
    void retrieveMailFromWorkContacts_notPresent(){
        final CertifiableFieldResourceOfstring email = new CertifiableFieldResourceOfstring(CertifiableFieldResourceOfstring.CertificationEnum.NONE, "email");
        WorkContactResource workContactResource = new WorkContactResource(email);
        Map<String, WorkContactResource> workContactResourceMap = Map.of("email", workContactResource);

        String mailFromWorkContact = userMapper.retrieveMailFromWorkContacts(workContactResourceMap, "notPresent");

        assertNull(mailFromWorkContact);
    }

    @Test
    void toWorkContactResponseMap(){
        final Map<String, WorkContactResource> workContactResourceMap = new HashMap<>();
        final WorkContactResource workContactResource = new WorkContactResource();
        final CertifiableFieldResourceOfstring email = new CertifiableFieldResourceOfstring(CertifiableFieldResourceOfstring.CertificationEnum.NONE, "email");
        workContactResource.setEmail(email);
        workContactResourceMap.put("email", workContactResource);
        Map<String, WorkContactResponse> responseMap = userMapper.toWorkContactResponse(workContactResourceMap);

        assertEquals(email.getValue(), responseMap.get("email").getEmail().getValue());
    }

    @Test
    void toWorkContactResponseMap_nullMap(){
        Map<String, WorkContactResponse> responseMap = userMapper.toWorkContactResponse(null);
        assertTrue(responseMap.isEmpty());
    }

    @Test
    void toWorkContactResponseMap_empty(){
        final Map<String, WorkContactResource> workContactResourceMap = new HashMap<>();

        Map<String, WorkContactResponse> responseMap = userMapper.toWorkContactResponse(workContactResourceMap);
        assertTrue(responseMap.isEmpty());

    }
}
