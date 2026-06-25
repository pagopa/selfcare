package it.pagopa.selfcare.mscore.connector.rest;

import it.pagopa.selfcare.mscore.api.UserApiConnector;
import it.pagopa.selfcare.mscore.connector.rest.client.UserApiRestClient;
import it.pagopa.selfcare.mscore.connector.rest.client.UserInstitutionApiRestClient;
import it.pagopa.selfcare.mscore.connector.rest.mapper.UserMapperClient;
import it.pagopa.selfcare.mscore.model.institution.InstitutionUpdate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class UserApiConnectorImpl implements UserApiConnector {

    private final UserApiRestClient userApiRestClient;

    private final UserInstitutionApiRestClient userInstitutionApiRestClient;

    private final UserMapperClient userMapper;

    public UserApiConnectorImpl(UserApiRestClient userApiRestClient, UserInstitutionApiRestClient userInstitutionApiRestClient, UserMapperClient userMapper) {
        this.userApiRestClient = userApiRestClient;
        this.userInstitutionApiRestClient = userInstitutionApiRestClient;
        this.userMapper = userMapper;
    }

    @Override
    public List<String> getUserEmails(String institutionId, String productId){
        ResponseEntity<List<String>> userEmails = userApiRestClient._usersEmailsGet(institutionId, productId);
        return userEmails.getBody();
    }

    @Override
    public void updateUserInstitution(String institutionId, InstitutionUpdate institutionUpdate){
       userInstitutionApiRestClient._institutionsInstitutionIdPut(institutionId, userMapper.toUpdateDescriptionDto(institutionUpdate));
    }

}
