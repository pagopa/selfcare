package it.pagopa.selfcare.onboarding.connector;

import feign.FeignException;
import it.pagopa.selfcare.commons.base.logging.LogUtils;
import it.pagopa.selfcare.onboarding.connector.api.UserRegistryConnector;
import it.pagopa.selfcare.onboarding.connector.model.user.MutableUserFieldsDto;
import it.pagopa.selfcare.onboarding.connector.model.user.SaveUserDto;
import it.pagopa.selfcare.onboarding.connector.model.user.User;
import it.pagopa.selfcare.onboarding.connector.model.user.UserId;
import it.pagopa.selfcare.onboarding.connector.rest.client.UserRegistryRestClient;
import it.pagopa.selfcare.onboarding.connector.rest.mapper.UserMapper;
import it.pagopa.selfcare.onboarding.connector.rest.model.user_registry.EmbeddedExternalId;
import it.pagopa.selfcare.user_registry.generated.openapi.v1.dto.UserSearchDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class UserRegistryConnectorImpl implements UserRegistryConnector {

    private final UserRegistryRestClient restClient;
    public static final String USERS_FIELD_LIST = "fiscalCode,familyName,name,workContacts";
    private final UserMapper userRegistryMapper;

    @Autowired
    public UserRegistryConnectorImpl(UserRegistryRestClient restClient, UserMapper userRegistryMapper) {
        this.restClient = restClient;
        this.userRegistryMapper = userRegistryMapper;
    }

    @Override
    public Optional<User> search(String externalId, EnumSet<User.Fields> fieldList) {
        log.trace("getUserByExternalId start");
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "getUserByExternalId externalId = {}", externalId);
        Assert.hasText(externalId, "A TaxCode is required");
        Assert.notEmpty(fieldList, "At least one user fields is required");
        Optional<User> user;
        try {
            user = Optional.of(restClient.search(new EmbeddedExternalId(externalId), fieldList));
        } catch (FeignException.NotFound e) {
            user = Optional.empty();
        }
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "getUserByExternalId result = {}", user);
        log.trace("getUserByExternalId end");

        return user;
    }

    @Override
    public User getUserByInternalId(String userId, EnumSet<User.Fields> fieldList) {
        log.trace("getUserByInternalId start");
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "getUserByInternalId userId = {}", userId);
        Assert.hasText(userId, "A userId is required");
        Assert.notEmpty(fieldList, "At least one user fields is required");
        User result = restClient.getUserByInternalId(UUID.fromString(userId), fieldList);
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "getUserByInternalId result = {}", result);
        log.trace("getUserByInternalId end");
        return result;
    }

    @Override
    public void updateUser(UUID id, MutableUserFieldsDto userDto) {
        log.trace("update start");
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "update id = {}, userDto = {}}", id, userDto);
        Assert.notNull(id, "A UUID is required");
        restClient.patchUser(id, userDto);
        log.trace("update end");
    }

    @Override
    public UserId saveUser(SaveUserDto dto) {
        log.trace("saveUser start");
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "saveUser dto = {}}", dto);
        UserId userId = restClient.saveUser(dto);
        log.debug("saveUser result = {}", userId);
        log.trace("saveUser end");
        return userId;
    }

    @Override
    public void deleteById(String userId) {
        log.trace("deleteById start");
        log.debug("deleteById id = {}", userId);
        Assert.hasText(userId, "A UUID is required");
        restClient.deleteById(UUID.fromString(userId));
        log.trace("deleteById end");
    }

    @Override
    public UserId searchUser(String taxCode) {
        log.trace("searchUser start");
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "searchUser taxCode = {}}", taxCode);
        UserId userId = userRegistryMapper.toUserId(restClient._searchUsingPOST(USERS_FIELD_LIST, new UserSearchDto().fiscalCode(taxCode)).getBody());
        log.debug("searchUser result = {}", userId);
        log.trace("searchUser end");
        return userId;
    }


}
