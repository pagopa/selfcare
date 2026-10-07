package it.pagopa.selfcare.onboarding.service;

import it.pagopa.selfcare.onboarding.client.UserRegistryRestClient;
import it.pagopa.selfcare.onboarding.client.model.EmbeddedExternalId;
import it.pagopa.selfcare.onboarding.client.model.MutableUserFieldsDto;
import it.pagopa.selfcare.onboarding.client.model.RegistryUser;
import it.pagopa.selfcare.onboarding.client.model.SaveUserDto;
import it.pagopa.selfcare.onboarding.client.model.UserId;
import it.pagopa.selfcare.onboarding.util.LogUtils;
import it.pagopa.selfcare.onboarding.util.Preconditions;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@ApplicationScoped
public class UserRegistryService {

    public static final String USERS_FIELD_LIST = "fiscalCode,familyName,name,workContacts";

    private final UserRegistryRestClient restClient;

    public UserRegistryService(@RestClient UserRegistryRestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * A registry 404 is raised as ResourceNotFoundException by the downstream error mapper, exactly as the
     * former FeignErrorDecoder did, so it is not turned into an empty result.
     */
    public Optional<RegistryUser> search(String externalId, EnumSet<RegistryUser.Fields> fieldList) {
        log.trace("getUserByExternalId start");
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "getUserByExternalId externalId = {}", externalId);
        Preconditions.hasText(externalId, "A TaxCode is required");
        Preconditions.notEmpty(fieldList, "At least one user fields is required");
        Optional<RegistryUser> user = Optional.of(
                restClient.search(new EmbeddedExternalId(externalId), UserRegistryRestClient.toFieldList(fieldList)));
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "getUserByExternalId result = {}", user);
        log.trace("getUserByExternalId end");
        return user;
    }

    public RegistryUser getUserByInternalId(String userId, EnumSet<RegistryUser.Fields> fieldList) {
        log.trace("getUserByInternalId start");
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "getUserByInternalId userId = {}", userId);
        Preconditions.hasText(userId, "A userId is required");
        Preconditions.notEmpty(fieldList, "At least one user fields is required");
        RegistryUser result = restClient.getUserByInternalId(UUID.fromString(userId), UserRegistryRestClient.toFieldList(fieldList));
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "getUserByInternalId result = {}", result);
        log.trace("getUserByInternalId end");
        return result;
    }

    public void updateUser(UUID id, MutableUserFieldsDto userDto) {
        log.trace("update start");
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "update id = {}, userDto = {}}", id, userDto);
        Preconditions.notNull(id, "A UUID is required");
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
        Preconditions.hasText(userId, "A UUID is required");
        restClient.deleteById(UUID.fromString(userId));
        log.trace("deleteById end");
    }

    public UserId searchUser(String taxCode) {
        log.trace("searchUser start");
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "searchUser taxCode = {}}", taxCode);
        RegistryUser found = restClient.search(new EmbeddedExternalId(taxCode), List.of(USERS_FIELD_LIST));
        UserId userId = new UserId();
        if (found != null && found.getId() != null) {
            userId.setId(UUID.fromString(found.getId()));
        }
        log.debug("searchUser result = {}", userId);
        log.trace("searchUser end");
        return userId;
    }
}
