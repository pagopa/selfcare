package it.pagopa.selfcare.mscore.connector.rest;

import it.pagopa.selfcare.commons.base.logging.LogUtils;
import it.pagopa.selfcare.mscore.api.UserRegistryConnector;
import it.pagopa.selfcare.mscore.connector.rest.client.UserRegistryRestClient;
import it.pagopa.selfcare.mscore.connector.rest.mapper.UserMapperClient;
import it.pagopa.selfcare.mscore.model.user.User;
import it.pagopa.selfcare.user_registry.generated.openapi.v1.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Map;
import java.util.Optional;


@Slf4j
@Service
public class UserRegistryConnectorImpl implements UserRegistryConnector {

    private final UserRegistryRestClient restClient;
    private final UserMapperClient userMapper;
    public static final String USERS_FIELD_LIST = "fiscalCode,name,familyName,workContacts";
    public static final String USERS_FIELD_LIST_WITHOUT_FISCAL_CODE = "name,familyName,workContacts";
    public static final String USER_REQUIRED_MESSAGE = "A userId is required";

    @Autowired
    public UserRegistryConnectorImpl(UserRegistryRestClient restClient, UserMapperClient userMapper) {
        this.restClient = restClient;
        this.userMapper = userMapper;
    }

    @Override
    public User getUserByInternalIdWithFiscalCode(String userId) {
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "getUserByInternalIdWithFiscalCode userId = {}", userId);
        Assert.hasText(userId, USER_REQUIRED_MESSAGE);
        ResponseEntity<UserResource> result = restClient._findByIdUsingGET(USERS_FIELD_LIST, userId);
        User user = userMapper.toUser(result.getBody());
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "getUserByInternalIdWithFiscalCode result = {}", result);
        return user;
    }

    @Override
    public User getUserByInternalId(String userId) {
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "getUserByInternalId userId = {}", userId);
        Assert.hasText(userId, USER_REQUIRED_MESSAGE);
        ResponseEntity<UserResource> result = restClient._findByIdUsingGET(USERS_FIELD_LIST_WITHOUT_FISCAL_CODE, userId);
        User user = userMapper.toUser(result.getBody());
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "getUserByInternalId result = {}", result);
        return user;
    }

    @Override
    public User getUserByInternalIdWithCustomFields(String userId, String fieldList) {
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "getUserByInternalId userId = {}", userId);
        Assert.hasText(userId, USER_REQUIRED_MESSAGE);
        ResponseEntity<UserResource> result = restClient._findByIdUsingGET(fieldList, userId);
        User user = userMapper.toUser(result.getBody());
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "getUserByInternalId result = {}", result);
        return user;
    }


    @Override
    public User getUserByFiscalCode(String fiscalCode) {
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "getUserByFiscalCode fiscalCode = {}", Encode.forJava(fiscalCode));
        Assert.hasText(fiscalCode, USER_REQUIRED_MESSAGE);
        UserResource result = restClient._searchUsingPOST(USERS_FIELD_LIST, new UserSearchDto().fiscalCode(fiscalCode))
                .getBody();
        User user = userMapper.toUser(result);
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "getUserByFiscalCode result = {}", result);
        return user;
    }

    @Override
    public User persistUserUsingPatch(String name, String familyName, String fiscalCode, String email, String institutionId) {
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "persistUserByFiscalCode fiscalCode = {}", fiscalCode);
        Assert.hasText(fiscalCode, "A fiscalCode is required");

        SaveUserDto.SaveUserDtoBuilder saveUserDtoBuilder = SaveUserDto.builder()
                .name(NameCertifiableSchema.builder()
                        .value(name)
                        .certification(NameCertifiableSchema.CertificationEnum.NONE)
                        .build())
                .familyName(FamilyNameCertifiableSchema.builder()
                        .value(familyName)
                        .certification(FamilyNameCertifiableSchema.CertificationEnum.NONE)
                        .build())
                .fiscalCode(fiscalCode);

        Optional.ofNullable(email).ifPresent(emailValue -> saveUserDtoBuilder
            .workContacts(Map.of(institutionId, WorkContactResource.builder()
                    .email(EmailCertifiableSchema.builder()
                            .value(emailValue)
                            .certification(EmailCertifiableSchema.CertificationEnum.NONE)
                            .build())
                    .build())));

        UserId result = restClient._saveUsingPATCH(saveUserDtoBuilder.build()).getBody();
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "persistUserByFiscalCode result = {}", result);
        return userMapper.fromUserId(result);
    }

    @Override
    public User persistUserWorksContractUsingPatch(String fiscalCode, String email, String institutionId) {
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "persistUserByFiscalCode fiscalCode = {}", fiscalCode);
        Assert.hasText(fiscalCode, "A fiscalCode is required");
        UserId result = restClient._saveUsingPATCH(SaveUserDto.builder()
                        .fiscalCode(fiscalCode)
                        .workContacts(Map.of(institutionId, WorkContactResource.builder()
                                .email(EmailCertifiableSchema.builder()
                                        .value(email)
                                        .certification(EmailCertifiableSchema.CertificationEnum.NONE)
                                        .build())
                                .build()))
                        .build())
                .getBody();
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "persistUserByFiscalCode result = {}", result);
        return userMapper.fromUserId(result);
    }


}
