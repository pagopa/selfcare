package it.pagopa.selfcare.mscore.connector.rest;


import it.pagopa.selfcare.mscore.connector.rest.client.UserRegistryRestClient;
import it.pagopa.selfcare.mscore.connector.rest.mapper.UserMapperClientImpl;
import it.pagopa.selfcare.mscore.model.user.User;
import it.pagopa.selfcare.user_registry.generated.openapi.v1.dto.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ContextConfiguration(classes = {UserRegistryConnectorImpl.class, UserMapperClientImpl.class})
@ExtendWith(SpringExtension.class)
class UserRegistryConnectorImplTest {
    @Autowired
    private UserRegistryConnectorImpl userRegistryConnector;

    @MockBean
    private UserRegistryRestClient userRegistryRestClient;

    @Test
    void getUserByInternalIdWithCustomFields(){
        UserResource userResource = new UserResource();
        userResource.setFiscalCode("42");
        ResponseEntity<UserResource> userIdResponseEntity = ResponseEntity.ok(userResource);
        when(userRegistryRestClient._findByIdUsingGET(any(), any())).thenReturn(userIdResponseEntity);
        User user = userRegistryConnector.getUserByInternalIdWithCustomFields("42","workContacts");
        assertEquals(user.getFiscalCode(), userResource.getFiscalCode());
        verify(userRegistryRestClient)._findByIdUsingGET("workContacts", "42");

    }

    /**
     * Method under test: {@link UserRegistryConnectorImpl#getUserByFiscalCode(String)}
     */
    @Test
    void testGetUserByFiscalCode() {
        UserResource userResource = new UserResource();
        userResource.setFiscalCode("42");
        ResponseEntity<UserResource> userIdResponseEntity = ResponseEntity.ok(userResource);
        when(userRegistryRestClient._searchUsingPOST(any(), any())).thenReturn(userIdResponseEntity);
        User user = userRegistryConnector.getUserByFiscalCode("42");
        assertEquals(user.getFiscalCode(), userResource.getFiscalCode());
        verify(userRegistryRestClient)._searchUsingPOST("fiscalCode,name,familyName,workContacts", UserSearchDto.builder().fiscalCode("42").build());
    }

    @Test
    void testGetUserByInternalIdWithFiscalCode() {
        UUID id = UUID.randomUUID();
        UserResource userResource = new UserResource();
        userResource.setId(id);
        ResponseEntity<UserResource> userIdResponseEntity = ResponseEntity.ok(userResource);
        when(userRegistryRestClient._findByIdUsingGET(any(), any())).thenReturn(userIdResponseEntity);
        User user = userRegistryConnector.getUserByInternalIdWithFiscalCode(id.toString());
        assertEquals(user.getId(), userResource.getId().toString());
        verify(userRegistryRestClient)._findByIdUsingGET("fiscalCode,name,familyName,workContacts", id.toString());
    }

    @Test
    void testPersistUserUsingPatch() {
        UUID id = UUID.randomUUID();
        UserId userId = new UserId();
        userId.setId(id);
        ResponseEntity<UserId> userIdResponseEntity = ResponseEntity.ok(userId);
        when(userRegistryRestClient._saveUsingPATCH(any())).thenReturn(userIdResponseEntity);
        User user = userRegistryConnector.persistUserUsingPatch("name", "familyName", "fiscalCode", "email","institutionId");
        assertEquals(user.getId(), id.toString());
        SaveUserDto saveUserDto = SaveUserDto.builder()
                .name(NameCertifiableSchema.builder()
                        .value("name")
                        .certification(NameCertifiableSchema.CertificationEnum.NONE)
                        .build())
                .familyName(FamilyNameCertifiableSchema.builder()
                        .value("familyName")
                        .certification(FamilyNameCertifiableSchema.CertificationEnum.NONE)
                        .build())
                .fiscalCode("fiscalCode")
                .workContacts(Map.of("institutionId", WorkContactResource.builder()
                        .email(EmailCertifiableSchema.builder()
                                .value("email")
                                .certification(EmailCertifiableSchema.CertificationEnum.NONE)
                                .build())
                        .build()))
                .build();
        verify(userRegistryRestClient)._saveUsingPATCH(saveUserDto);
    }

    @Test
    void testPersistUserUsingPatchWithEmailNull() {
        UUID id = UUID.randomUUID();
        UserId userId = new UserId();
        userId.setId(id);
        ResponseEntity<UserId> userIdResponseEntity = ResponseEntity.ok(userId);
        when(userRegistryRestClient._saveUsingPATCH(any())).thenReturn(userIdResponseEntity);
        User user = userRegistryConnector.persistUserUsingPatch("name", "familyName", "fiscalCode", null,"institutionId");
        assertEquals(user.getId(), id.toString());
        SaveUserDto saveUserDto = SaveUserDto.builder()
                .name(NameCertifiableSchema.builder()
                        .value("name")
                        .certification(NameCertifiableSchema.CertificationEnum.NONE)
                        .build())
                .familyName(FamilyNameCertifiableSchema.builder()
                        .value("familyName")
                        .certification(FamilyNameCertifiableSchema.CertificationEnum.NONE)
                        .build())
                .fiscalCode("fiscalCode")
                .build();
        verify(userRegistryRestClient)._saveUsingPATCH(saveUserDto);
    }

    @Test
    void persistUserWorksContractUsingPatch() {
        UUID id = UUID.randomUUID();
        UserId userId = new UserId();
        userId.setId(id);
        ResponseEntity<UserId> userIdResponseEntity = ResponseEntity.ok(userId);
        when(userRegistryRestClient._saveUsingPATCH(any())).thenReturn(userIdResponseEntity);
        User user = userRegistryConnector.persistUserWorksContractUsingPatch( "fiscalCode", "email","institutionId");
        assertEquals(user.getId(), id.toString());
        SaveUserDto saveUserDto = SaveUserDto.builder()
                .fiscalCode("fiscalCode")
                .workContacts(Map.of("institutionId", WorkContactResource.builder()
                        .email(EmailCertifiableSchema.builder()
                                .value("email")
                                .certification(EmailCertifiableSchema.CertificationEnum.NONE)
                                .build())
                        .build()))
                .build();
        verify(userRegistryRestClient)._saveUsingPATCH(saveUserDto);
    }

    @Test
    void testGetUserById(){
        UUID id = UUID.randomUUID();
        UserResource userResource = new UserResource();
        userResource.setId(id);
        ResponseEntity<UserResource> userIdResponseEntity = ResponseEntity.ok(userResource);
        when(userRegistryRestClient._findByIdUsingGET(any(), any())).thenReturn(userIdResponseEntity);
        User user = userRegistryConnector.getUserByInternalId(id.toString());
        assertEquals(user.getId(), userResource.getId().toString());
        verify(userRegistryRestClient)._findByIdUsingGET("name,familyName,workContacts", id.toString());
    }

}

