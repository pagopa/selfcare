package it.pagopa.selfcare.user_group.service;

import it.pagopa.selfcare.user_group.api.UserGroupOperations;
import it.pagopa.selfcare.user_group.model.UserGroupFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserGroupService {

    UserGroupOperations createGroup(UserGroupOperations group);

    void addMember(String id, UUID memberId);

    void deleteMember(String groupId, String memberId);

    void deleteMembers(String userId, String institutionId, String memberId);

    UserGroupOperations getUserGroup(String id);

    Page<UserGroupOperations> getUserGroups(UserGroupFilter filter, Pageable pageable);

    void deleteGroup(String id);

    void suspendGroup(String id);

    void activateGroup(String id);

    UserGroupOperations updateGroup(String id, UserGroupOperations group);
}
