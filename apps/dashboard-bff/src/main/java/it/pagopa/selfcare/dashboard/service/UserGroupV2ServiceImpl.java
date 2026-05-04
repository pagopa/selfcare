package it.pagopa.selfcare.dashboard.service;

import it.pagopa.selfcare.commons.base.logging.LogUtils;
import it.pagopa.selfcare.dashboard.client.UserApiRestClient;
import it.pagopa.selfcare.dashboard.client.UserGroupRestClient;
import it.pagopa.selfcare.dashboard.client.UserInstitutionApiRestClient;
import it.pagopa.selfcare.dashboard.exception.InvalidMemberListException;
import it.pagopa.selfcare.dashboard.exception.InvalidUserGroupException;
import it.pagopa.selfcare.dashboard.exception.ResourceNotFoundException;
import it.pagopa.selfcare.dashboard.model.groups.*;
import it.pagopa.selfcare.dashboard.model.mapper.GroupMapper;
import it.pagopa.selfcare.dashboard.model.mapper.UserMapper;
import it.pagopa.selfcare.dashboard.model.user.User;
import it.pagopa.selfcare.dashboard.model.user.UserInfo;
import it.pagopa.selfcare.dashboard.model.user.UserInstitution;
import it.pagopa.selfcare.dashboard.utils.EncodingUtils;
import it.pagopa.selfcare.group.generated.openapi.v1.dto.CreateUserGroupDto;
import it.pagopa.selfcare.group.generated.openapi.v1.dto.PageOfUserGroupResource;
import it.pagopa.selfcare.group.generated.openapi.v1.dto.UpdateUserGroupDto;
import it.pagopa.selfcare.group.generated.openapi.v1.dto.UserGroupResource;
import it.pagopa.selfcare.user.generated.openapi.v1.dto.OnboardedProductState;
import it.pagopa.selfcare.user.generated.openapi.v1.dto.UserDataResponse;
import it.pagopa.selfcare.user.generated.openapi.v1.dto.UserInstitutionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Stream;

import static it.pagopa.selfcare.dashboard.model.institution.RelationshipState.ACTIVE;
import static it.pagopa.selfcare.dashboard.model.institution.RelationshipState.SUSPENDED;
import static it.pagopa.selfcare.dashboard.model.user.User.Fields.familyName;
import static it.pagopa.selfcare.dashboard.model.user.User.Fields.name;

@Component
@Slf4j
@RequiredArgsConstructor
public class UserGroupV2ServiceImpl implements UserGroupV2Service {

    private static final EnumSet<User.Fields> FIELD_LIST = EnumSet.of(name, familyName);

    private final UserGroupRestClient userGroupRestClient;
    private final UserInstitutionApiRestClient userInstitutionApiRestClient;
    private final UserApiRestClient userApiRestClient;

    private final UserMapper userMapper;
    private final GroupMapper groupMapper;

    public static final String REQUIRED_GROUP_ID_MESSAGE = "A user group id is required";

    @Override
    public String createUserGroup(CreateUserGroup group) {
        log.trace("createUserGroup start");
        log.debug("createUserGroup group = {}", group);
        UserInfo.UserInfoFilter userInfoFilter = new UserInfo.UserInfoFilter();
        userInfoFilter.setProductId(group.getProductId());
        userInfoFilter.setAllowedStates(List.of(ACTIVE, SUSPENDED));

        List<String> retrievedId = retrieveIds(group.getInstitutionId(), userInfoFilter);

        if (group.getMembers().stream()
                .filter(uuid -> Collections.binarySearch(retrievedId, uuid) >= 0)
                .count() != group.getMembers().size()) {
            throw new InvalidMemberListException("Some members in the list aren't allowed for this institution");
        }
        Assert.notNull(group, "A User Group is required");
        CreateUserGroupDto userGroupDto = groupMapper.toCreateUserGroupDto(group);
        userGroupDto.setStatus(CreateUserGroupDto.StatusEnum.ACTIVE);
        UserGroupResource userGroupResource = userGroupRestClient._createGroupUsingPOST(userGroupDto).getBody();
        String groupId = userGroupResource != null ? userGroupResource.getId() : null;
        log.debug("createUserGroup result = {}", groupId);
        log.trace("createUserGroup end");
        return groupId;
    }


    @Override
    public void delete(String groupId) {
        log.trace("delete start");
        log.debug("delete groupId = {}", Encode.forJava(groupId));
        Assert.hasText(groupId, REQUIRED_GROUP_ID_MESSAGE);
        userGroupRestClient._deleteGroupUsingDELETE(groupId);
        log.trace("delete end");
    }

    @Override
    public void activate(String groupId) {
        log.trace("activate start");
        log.debug("activate groupId = {}", Encode.forJava(groupId));
        Assert.hasText(groupId, REQUIRED_GROUP_ID_MESSAGE);
        userGroupRestClient._activateGroupUsingPOST(groupId);
        log.trace("activate end");
    }

    private List<String> retrieveIds(String institutionId, UserInfo.UserInfoFilter userInfoFilter) {
        List<String> retrievedUsers = retrieveFilteredUserInstitution(
                institutionId,
                userInfoFilter);
        return retrievedUsers.stream()
                .sorted()
                .toList();
    }

    private List<String> retrieveFilteredUserInstitution(String institutionId, UserInfo.UserInfoFilter userInfoFilter) {
        return Optional.ofNullable(userInstitutionApiRestClient._retrieveUserInstitutions(institutionId,
                                null,
                                List.of(userInfoFilter.getProductId()),
                                null,
                                Optional.ofNullable(userInfoFilter.getAllowedStates())
                                        .map(relationshipStates -> relationshipStates.stream().map(Enum::name).toList())
                                        .orElse(null),
                                null)
                        .getBody()).map(userInstitutionResponses -> userInstitutionResponses.stream()
                        .map(UserInstitutionResponse::getUserId).toList())
                .orElse(Collections.emptyList());
    }

    @Override
    public void suspend(String groupId) {
        log.trace("suspend start");
        log.debug("suspend groupId = {}", Encode.forJava(groupId));
        Assert.hasText(groupId, REQUIRED_GROUP_ID_MESSAGE);
        userGroupRestClient._suspendGroupUsingPOST(groupId);
        log.trace("suspend end");
    }

    @Override
    public void updateUserGroup(String groupId, UpdateUserGroup group) {
        log.trace("updateUserGroup start");
        log.debug("updateUserGroup groupId = {}, group = {}", Encode.forJava(groupId), group);
        UserGroupInfo userGroupInfo = groupMapper.toUserGroupInfo(userGroupRestClient._getUserGroupUsingGET(groupId).getBody());
        log.debug("getUseGroupById groupInfo = {}", userGroupInfo);

        UserInfo.UserInfoFilter userInfoFilter = new UserInfo.UserInfoFilter();
        userInfoFilter.setProductId(userGroupInfo.getProductId());
        userInfoFilter.setAllowedStates(List.of(ACTIVE, SUSPENDED));

        List<String> retrievedId = retrieveIds(userGroupInfo.getInstitutionId(), userInfoFilter);

        if (group.getMembers().stream()
                .filter(uuid -> Collections.binarySearch(retrievedId, uuid) >= 0)
                .count() != group.getMembers().size()) {
            throw new InvalidMemberListException("Some members in the list aren't allowed for this institution");
        }
        UpdateUserGroupDto updateUserGroupDto = groupMapper.toUpdateUserGroupDto(group);
        userGroupRestClient._updateUserGroupUsingPUT(groupId, updateUserGroupDto);
        log.trace("updateUserGroup end");
    }

    @Override
    public void addMemberToUserGroup(String groupId, UUID userId) {
        log.trace("addMemberToUserGroup start");
        log.debug("addMemberToUserGroup groupId = {}, userId = {}", Encode.forJava(groupId), userId);
        Assert.hasText(groupId, REQUIRED_GROUP_ID_MESSAGE);
        Assert.notNull(userId, "A userId is required");

        it.pagopa.selfcare.group.generated.openapi.v1.dto.UserGroupResource response = userGroupRestClient._getUserGroupUsingGET(groupId).getBody();
        UserGroupInfo retrievedGroup = groupMapper.toUserGroupInfo(response);
        log.debug("getUseGroupById groupInfo = {}", retrievedGroup);
        log.trace("getUserGroupById end");

        UserInfo.UserInfoFilter userInfoFilter = new UserInfo.UserInfoFilter();
        userInfoFilter.setProductId(retrievedGroup.getProductId());
        userInfoFilter.setAllowedStates(List.of(ACTIVE, SUSPENDED));
        List<String> retrievedIds = retrieveIds(retrievedGroup.getInstitutionId(), userInfoFilter);
        if (!retrievedIds.contains(userId.toString())) {
            throw new InvalidMemberListException("This user is not allowed for this group");
        }
        userGroupRestClient._addMemberToUserGroupUsingPUT(groupId, userId);
        log.trace("addMemberToUserGroup end");
    }

    @Override
    public void deleteMemberFromUserGroup(String groupId, UUID userId) {
        log.trace("deleteMemberFromUserGroup start");
        log.debug("deleteMemberFromUserGroup groupId = {}, userId = {}", Encode.forJava(groupId), userId);
        Assert.hasText(groupId, REQUIRED_GROUP_ID_MESSAGE);
        Assert.notNull(userId, "A userId is required");
        userGroupRestClient._deleteMemberFromUserGroupUsingDELETE(groupId, userId);
        log.trace("deleteMemberFromUserGroup end");
    }

    @Override
    public UserGroupInfo getUserGroupById(String groupId) {
        log.trace("getUserGroupById start");
        log.debug("getUserGroupById groupId = {}", Encode.forJava(groupId));
        Assert.hasText(groupId, REQUIRED_GROUP_ID_MESSAGE);
        UserGroupResource response = userGroupRestClient._getUserGroupUsingGET(groupId).getBody();
        UserGroupInfo userGroupInfo = groupMapper.toUserGroupInfo(response);

        final String institutionId = userGroupInfo.getInstitutionId();
        List<UserInfo> members = new ArrayList<>();

        userGroupInfo.getMembers().forEach(user -> {
            UserInfo member = getUserByUserIdInstitutionIdAndProductAndStates(user.getId(), institutionId, userGroupInfo.getProductId(), List.of(ACTIVE.name(), SUSPENDED.name()));
            members.add(member);
        });

        userGroupInfo.setMembers(members);

        // createdBy
        Optional.ofNullable(userGroupInfo.getCreatedBy()).filter(user -> EncodingUtils.isUUID(user.getId())).ifPresentOrElse(user -> {
            User createdBy = getUserById(user.getId(), institutionId, FIELD_LIST.stream().map(Enum::name).toList());
            userGroupInfo.setCreatedBy(createdBy);
        }, () -> userGroupInfo.setCreatedBy(null));
        // modifiedBy
        Optional.ofNullable(userGroupInfo.getModifiedBy()).filter(user -> EncodingUtils.isUUID(user.getId())).ifPresentOrElse(user -> {
            User modifiedBy = getUserById(user.getId(), institutionId, FIELD_LIST.stream().map(Enum::name).toList());
            userGroupInfo.setModifiedBy(modifiedBy);
        }, () -> userGroupInfo.setModifiedBy(null));

        return userGroupInfo;
    }

    @Override
    public UserGroupInfo getUserGroupById(String groupId, String memberId) {
        final UserGroupInfo userGroup = getUserGroupById(groupId);
        return Optional.of(userGroup)
                .filter(g -> userGroup.getMembers().stream().anyMatch(u -> u.getId().equals(memberId)))
                .orElseThrow(() -> new InvalidUserGroupException("User is not member of group"));
    }

    private UserInfo getUserByUserIdInstitutionIdAndProductAndStates(String userId, String institutionId, String productId, List<String> states) {
        log.trace("getUserByUserIdInstitutionIdAndProduct start");
        List<UserDataResponse> institutionResponses = userApiRestClient._retrieveUsers(institutionId, userId, userId, null, List.of(productId), null, states)
                .getBody();

        if (CollectionUtils.isEmpty(institutionResponses) || institutionResponses.size() != 1) {
            throw new ResourceNotFoundException(String.format("InstitutionId %s and userId %s not found", institutionId, userId));
        }

        log.debug("getProducts result = {}", institutionResponses);
        log.trace("getProducts end");
        return userMapper.toUserInfo(institutionResponses.get(0));
    }

    private User getUserById(String userId, String institutionId, List<String> fields) {
        log.trace("getUserById start");
        log.debug("getUserById id = {}", userId);
        String fieldsString = !CollectionUtils.isEmpty(fields) ? String.join(",", fields) : null;
        User user = userMapper.toUser(userApiRestClient._getUserDetailsById(userId, fieldsString, institutionId).getBody());
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "getUserById = {}", user);
        log.trace("getUserById end");
        return user;
    }

    @Override
    public Page<UserGroup> getUserGroups(String institutionId, String productId, UUID userId, Pageable pageable) {
        log.trace("getUserGroups start");
        log.debug("getUserGroups institutionId = {}, productId = {}, userId = {}, pageable = {}",
                Encode.forJava(institutionId),
                Encode.forJava(productId),
                Encode.forJava(Optional.ofNullable(userId).map(Object::toString).orElse("")),
                Encode.forJava(Optional.ofNullable(pageable).map(Object::toString).orElse(""))
        );
        UserGroupFilter userGroupFilter = new UserGroupFilter();
        userGroupFilter.setInstitutionId(Optional.ofNullable(institutionId));
        userGroupFilter.setUserId(Optional.ofNullable(userId));
        userGroupFilter.setProductId(Optional.ofNullable(productId));
        Page<UserGroup> groupInfos = getUserGroups(userGroupFilter, pageable);
        log.debug("getUserGroups result = {}", groupInfos);
        log.trace("getUserGroups end");

        return groupInfos;
    }

    public Page<UserGroup> getUserGroups(UserGroupFilter filter, Pageable pageable) {
        List<String> sortParams = new ArrayList<>();
        if (pageable.getSort().isSorted()) {
            pageable.getSort().forEach(order -> sortParams.add(order.getProperty() + "," + order.getDirection()));
        }

        final PageOfUserGroupResource userGroupResources = userGroupRestClient._getUserGroupsUsingGET(filter.getInstitutionId().orElse(null),
                pageable.getPageNumber(),
                pageable.getPageSize(),
                sortParams,
                filter.getProductId().orElse(null),
                filter.getUserId().orElse(null),
                String.join(",", UserGroupStatus.ACTIVE.name(), UserGroupStatus.SUSPENDED.name())).getBody();

        assert userGroupResources != null;
        return convertToUserGroupInfoPage(userGroupResources, pageable);
    }

    private Page<UserGroup> convertToUserGroupInfoPage(PageOfUserGroupResource userGroupResources, Pageable pageable) {
        return new PageImpl<>(
                userGroupResources.getContent().stream().map(groupMapper::toUserGroup).toList(),
                pageable,
                userGroupResources.getTotalElements()
        );
    }

    @Override
    public void deleteMembersByUserId(String userId, String institutionId, String productId) {
        log.trace("deleteMembersByUserId start");
        log.debug("deleteMembersByUserId userId = {}", userId);
        List<UserInstitution> userInstitutionList = retrieveFilteredUser(userId, institutionId, productId);
        if (CollectionUtils.isEmpty(userInstitutionList)) {
            log.debug("User not found, deleting members for userId = {}", userId);
            userGroupRestClient._deleteMemberFromUserGroupsUsingDELETE(UUID.fromString(userId), institutionId, productId);
        } else {
            log.debug("User found, not deleting members for userId = {}", userId);
        }
    }

    private List<UserInstitution> retrieveFilteredUser(String userId, String institutionId, String productId) {
        log.trace("retrieveFilteredUser start");
        log.debug("retrieveFilteredUser userId = {}, institutionId = {}, productId = {}", userId, institutionId, productId);
        List<UserInstitutionResponse> institutionResponses = userInstitutionApiRestClient._retrieveUserInstitutions(institutionId, null, List.of(productId), null, getValidUserStates(), userId).getBody();
        if (!CollectionUtils.isEmpty(institutionResponses)) {
            log.info("retrieveFilteredUser institutionResponses size = {}", institutionResponses.size());
            return institutionResponses.stream()
                    .map(userMapper::toUserInstitution)
                    .toList();
        }
        return Collections.emptyList();
    }

    private List<String> getValidUserStates() {
        return Stream.of(OnboardedProductState.values())
                .filter(onboardedProductState -> onboardedProductState != OnboardedProductState.DELETED && onboardedProductState != OnboardedProductState.REJECTED)
                .map(Enum::name)
                .toList();
    }
}
