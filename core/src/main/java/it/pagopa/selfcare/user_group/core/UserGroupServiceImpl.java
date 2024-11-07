package it.pagopa.selfcare.user_group.core;

import it.pagopa.selfcare.commons.base.security.SelfCareUser;
import it.pagopa.selfcare.user_group.connector.api.UserGroupConnector;
import it.pagopa.selfcare.user_group.connector.api.UserGroupOperations;
import it.pagopa.selfcare.user_group.connector.exception.ResourceAlreadyExistsException;
import it.pagopa.selfcare.user_group.connector.exception.ResourceNotFoundException;
import it.pagopa.selfcare.user_group.connector.exception.ResourceUpdateException;
import it.pagopa.selfcare.user_group.connector.model.UserGroupFilter;
import it.pagopa.selfcare.user_group.connector.model.UserGroupStatus;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.validation.ValidationException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
class UserGroupServiceImpl implements UserGroupService {

    private final UserGroupConnector groupConnector;
    private static final String USER_GROUP_ID_REQUIRED_MESSAGE = "A user group id is required";
    private static final String TRYING_TO_MODIFY_SUSPENDED_GROUP = "Trying to modify suspended group";
    private static final String MEMBER_ID_REQUIRED = "A member id is required";
    private static final String GROUP_NAME_ALREADY_EXISTS = "A group with the same name already exists in ACTIVE or SUSPENDED state";
    private final List<String> allowedSortingParams;

    @Autowired
    UserGroupServiceImpl(UserGroupConnector groupConnector,
                         @Value("${user-group.allowed.sorting.parameters}") String[] allowedSortingParams) {
        this.groupConnector = groupConnector;
        this.allowedSortingParams = Arrays.asList(allowedSortingParams);
    }

    @Override
    public UserGroupOperations createGroup(UserGroupOperations group) {
        log.trace("createGroup start");
        log.debug("createGroup group = {}", group);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Assert.state(authentication != null, "Authentication is required");
        Assert.state(authentication.getPrincipal() instanceof SelfCareUser, "Not SelfCareUser principal");
        Assert.notNull(group, "A group is required");

        checkNameUniqueness(group.getId(), group.getName(), group.getProductId(), group.getInstitutionId());
        UserGroupOperations insert = groupConnector.insert(group);
        log.debug("insert = {}", insert);
        log.trace("createGroup end");
        return insert;
    }

    private void checkNameUniqueness(String currentGroupId, String groupName, String productId, String institutionId) {
        UserGroupFilter filter = new UserGroupFilter();
        filter.setProductId(productId);
        filter.setInstitutionId(institutionId);
        filter.setStatus(Arrays.asList(UserGroupStatus.ACTIVE, UserGroupStatus.SUSPENDED));

        Page<UserGroupOperations> existingGroups = groupConnector.findAll(filter, Pageable.unpaged());
        boolean isDuplicate = existingGroups.stream()
                .anyMatch(g -> g.getName().equals(groupName) && !g.getId().equals(currentGroupId));

        if (isDuplicate) {
            log.warn("Attempted to create/update group with duplicate name: {}", groupName);
            throw new ResourceAlreadyExistsException(GROUP_NAME_ALREADY_EXISTS);
        }
    }

    @Override
    public void addMember(String id, UUID memberId) {
        log.trace("addMember start");
        log.debug("addMember id = {}, memberId ={}", Encode.forJava(id), memberId);
        Assert.hasText(id, USER_GROUP_ID_REQUIRED_MESSAGE);
        Assert.notNull(memberId, MEMBER_ID_REQUIRED);
        UserGroupOperations foundGroup = groupConnector.findById(id).orElseThrow(ResourceNotFoundException::new);
        if (UserGroupStatus.SUSPENDED.equals(foundGroup.getStatus())) {
            throw new ResourceUpdateException(TRYING_TO_MODIFY_SUSPENDED_GROUP);
        }
        groupConnector.insertMember(id, memberId.toString());
        log.trace("addMember end");
    }

    @Override
    public void deleteMember(String groupId, String memberId) {
        log.trace("deleteMember start");
        log.debug("deleteMember groupId = {}, memberId = {}", Encode.forJava(groupId), Encode.forJava(memberId));
        Assert.hasText(groupId, USER_GROUP_ID_REQUIRED_MESSAGE);
        Assert.hasText(memberId, MEMBER_ID_REQUIRED);
        UserGroupOperations foundGroup = groupConnector.findById(groupId).orElseThrow(ResourceNotFoundException::new);
        if (UserGroupStatus.SUSPENDED.equals(foundGroup.getStatus())) {
            throw new ResourceUpdateException(TRYING_TO_MODIFY_SUSPENDED_GROUP);
        }
        groupConnector.deleteMember(groupId, memberId);
        log.trace("deleteMember end");
    }

    @Override
    public void deleteMembers(String memberId, String institutionId, String productId) {
        log.trace("deleteMembers start");
        log.debug("deleteMembers memberId = {}, institutionId = {}, productId= {}", Encode.forJava(memberId), Encode.forJava(institutionId), Encode.forJava(productId));
        Assert.hasText(memberId, MEMBER_ID_REQUIRED);
        Assert.hasText(institutionId, "A institution id is required");
        Assert.hasText(productId, "A product id is required");
        groupConnector.deleteMembers(memberId, institutionId, productId);
        log.trace("deleteMembers end");
    }

    @Override
    public UserGroupOperations getUserGroup(String id) {
        log.trace("getUserGroup start");
        log.debug("getUserGroup id = {}", id);
        Assert.hasText(id, USER_GROUP_ID_REQUIRED_MESSAGE);
        UserGroupOperations foundGroup = groupConnector.findById(id).orElseThrow(ResourceNotFoundException::new);
        log.debug("getUserGroup result = {}", foundGroup);
        log.trace("getUserGroup end");

        return foundGroup;
    }


    @Override
    public Page<UserGroupOperations> getUserGroups(UserGroupFilter filter, Pageable pageable) {
        log.trace("getUserGroups start");
        log.debug("getUserGroups filter = {}, pageable = {}", filter, pageable);
        boolean match = pageable.getSort().stream().allMatch(order -> allowedSortingParams.contains(order.getProperty()));
        if (!match) {
            throw new ValidationException("Given sort parameters aren't valid");
        }
        Page<UserGroupOperations> result = groupConnector.findAll(filter, pageable);
        log.debug("getUserGroups result = {}", result);
        log.trace("getUserGroups end");
        return result;
    }


    @Override
    public void deleteGroup(String id) {
        log.trace("deleteGroup start");
        log.debug("deleteGroup id = {}", Encode.forJava(id));
        Assert.hasText(id, USER_GROUP_ID_REQUIRED_MESSAGE);
        groupConnector.deleteById(id);
        log.trace("deleteProduct end");
    }

    @Override
    public void suspendGroup(String id) {
        log.trace("suspendGroup start");
        log.debug("suspendGroup id = {}", Encode.forJava(id));
        Assert.hasText(id, USER_GROUP_ID_REQUIRED_MESSAGE);
        groupConnector.suspendById(id);
        log.trace("suspendGroup end");
    }

    @Override
    public void activateGroup(String id) {
        log.trace("activateGroup start");
        log.debug("activateGroup id = {}", Encode.forJava(id));
        Assert.hasText(id, USER_GROUP_ID_REQUIRED_MESSAGE);
        groupConnector.activateById(id);
        log.trace("activateGroup end");
    }

    @Override
    public UserGroupOperations updateGroup(String id, UserGroupOperations group) {
        log.trace("updateGroup start");
        log.debug("updateGroup id = {}, group = {}", Encode.forJava(id), group);
        Assert.hasText(id, USER_GROUP_ID_REQUIRED_MESSAGE);
        Assert.notNull(group, "A user group is required");
        UserGroupOperations foundGroup = groupConnector.findById(id).orElseThrow(ResourceNotFoundException::new);
        if (UserGroupStatus.SUSPENDED.equals(foundGroup.getStatus())) {
            throw new ResourceUpdateException(TRYING_TO_MODIFY_SUSPENDED_GROUP);
        }
        checkNameUniqueness(id, group.getName(), foundGroup.getProductId(), foundGroup.getInstitutionId());

        foundGroup.setMembers(group.getMembers());
        foundGroup.setName(group.getName());
        foundGroup.setDescription(group.getDescription());
        UserGroupOperations updatedGroup = groupConnector.save(foundGroup);
        log.debug("updateGroup updatedGroup = {}", updatedGroup);
        log.trace("updateGroup end");
        return updatedGroup;
    }
}
