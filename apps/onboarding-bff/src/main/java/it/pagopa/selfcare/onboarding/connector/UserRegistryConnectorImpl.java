package it.pagopa.selfcare.onboarding.connector;

import feign.FeignException;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import it.pagopa.selfcare.commons.base.logging.LogUtils;
import it.pagopa.selfcare.onboarding.connector.model.user.MutableUserFieldsDto;
import it.pagopa.selfcare.onboarding.connector.model.user.SaveUserDto;
import it.pagopa.selfcare.onboarding.connector.model.user.User;
import it.pagopa.selfcare.onboarding.connector.model.user.UserId;
import it.pagopa.selfcare.onboarding.connector.rest.client.UserRegistryRestClient;
import it.pagopa.selfcare.onboarding.connector.rest.mapper.UserMapper;
import it.pagopa.selfcare.onboarding.connector.rest.model.user_registry.EmbeddedExternalId;
import org.openapi.quarkus.user_registry_json.model.UserSearchDto;
import lombok.extern.slf4j.Slf4j;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@ApplicationScoped
public class UserRegistryConnectorImpl {

    private final UserRegistryRestClient restClient;
    public static final String USERS_FIELD_LIST = "fiscalCode,familyName,name,workContacts";
    private final UserMapper userRegistryMapper;
    public UserRegistryConnectorImpl(@RestClient UserRegistryRestClient restClient, UserMapper userRegistryMapper) {
        this.restClient = restClient;
        this.userRegistryMapper = userRegistryMapper;
    }
    public Optional<User> search(String externalId, EnumSet<User.Fields> fieldList) {
        log.trace("getUserByExternalId start");
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "getUserByExternalId externalId = {}", externalId);
        requireHasText(externalId, "A TaxCode is required");
        requireNotEmpty(fieldList, "At least one user fields is required");
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
    public User getUserByInternalId(String userId, EnumSet<User.Fields> fieldList) {
        log.trace("getUserByInternalId start");
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "getUserByInternalId userId = {}", userId);
        requireHasText(userId, "A userId is required");
        requireNotEmpty(fieldList, "At least one user fields is required");
        User result = restClient.getUserByInternalId(UUID.fromString(userId), fieldList);
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "getUserByInternalId result = {}", result);
        log.trace("getUserByInternalId end");
        return result;
    }
    public void updateUser(UUID id, MutableUserFieldsDto userDto) {
        log.trace("update start");
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "update id = {}, userDto = {}}", id, userDto);
        if (id == null) {
            throw new IllegalArgumentException("A UUID is required");
        }
        restClient.patchUser(id, userDto);
        log.trace("update end");
    }
    public UserId saveUser(SaveUserDto dto) {
        log.trace("saveUser start");
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "saveUser dto = {}}", dto);
        UserId userId = restClient.saveUser(dto);
        log.debug("saveUser result = {}", userId);
        log.trace("saveUser end");
        return userId;
    }
    public void deleteById(String userId) {
        log.trace("deleteById start");
        log.debug("deleteById id = {}", userId);
        requireHasText(userId, "A UUID is required");
        restClient.deleteById(UUID.fromString(userId));
        log.trace("deleteById end");
    }
    public UserId searchUser(String taxCode) {
        log.trace("searchUser start");
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "searchUser taxCode = {}}", taxCode);
        UserId userId = userRegistryMapper.toUserId(restClient._searchUsingPOST(USERS_FIELD_LIST, new UserSearchDto().fiscalCode(taxCode)));
        log.debug("searchUser result = {}", userId);
        log.trace("searchUser end");
        return userId;
    }


    private static void requireHasText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void requireNotEmpty(Collection<?> values, String message) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }
}
