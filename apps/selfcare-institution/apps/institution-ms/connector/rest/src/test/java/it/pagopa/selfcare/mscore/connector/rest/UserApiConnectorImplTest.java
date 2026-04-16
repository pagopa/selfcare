package it.pagopa.selfcare.mscore.connector.rest;

import it.pagopa.selfcare.mscore.connector.rest.client.UserApiRestClient;
import it.pagopa.selfcare.mscore.connector.rest.client.UserInstitutionApiRestClient;
import it.pagopa.selfcare.mscore.connector.rest.mapper.UserMapperClient;
import it.pagopa.selfcare.mscore.model.institution.InstitutionUpdate;
import it.pagopa.selfcare.user.generated.openapi.v1.dto.UpdateDescriptionDto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ContextConfiguration(classes = {UserApiConnectorImpl.class})
@ExtendWith(SpringExtension.class)
class UserApiConnectorImplTest {

    @Autowired
    private UserApiConnectorImpl userApiConnector;

    @MockBean
    private UserApiRestClient userApiRestClient;

    @MockBean
    private UserInstitutionApiRestClient userInstitutionApiRestClient;

    @MockBean
    private UserMapperClient userMapperClient;

    @Test
    void getUserEmailsByInstitutionAndProduct(){
        //given
        final String institutionId = "institutionId";
        final String productId  = "productId";
        ResponseEntity<List<String>> emails = new ResponseEntity<>(List.of("email"), HttpStatus.OK);

        when(userApiRestClient._usersEmailsGet(anyString(), anyString())).thenReturn(emails);

        //when
        List<String> userEmails = userApiConnector.getUserEmails(institutionId, productId);
        //then
        assertFalse(userEmails.isEmpty());
        verify(userApiRestClient, times(1))._usersEmailsGet(institutionId, productId);

    }

    @Test
    void updateUserInstitution(){
        //given
        final String institutionId = "institutionId";
        final String description = "description";
        final String rootName = "rootName";

        UpdateDescriptionDto descriptionDto = new UpdateDescriptionDto();
        descriptionDto.setInstitutionDescription(description);
        descriptionDto.setInstitutionRootName(rootName);

        InstitutionUpdate institutionUpdate = new InstitutionUpdate();
        institutionUpdate.setDescription(description);
        institutionUpdate.setParentDescription(rootName);

        when(userMapperClient.toUpdateDescriptionDto(any())).thenReturn(descriptionDto);

        //when
        final Executable executable = () -> userApiConnector.updateUserInstitution(institutionId, institutionUpdate);

        //then
        Assertions.assertDoesNotThrow(executable);
        verify(userInstitutionApiRestClient)._institutionsInstitutionIdPut(institutionId, descriptionDto);
    }



}