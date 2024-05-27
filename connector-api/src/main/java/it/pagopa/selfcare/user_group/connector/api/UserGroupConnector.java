package it.pagopa.selfcare.user_group.connector.api;

import it.pagopa.selfcare.user_group.connector.model.UserGroupFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface UserGroupConnector {
    UserGroupOperations insert(UserGroupOperations entity);

    UserGroupOperations save(UserGroupOperations entity);

    void insertMember(String id, String memberId);

    void deleteMember(String id, String memberId);

    void deleteMembers(String memberId, String institutionId, String productId);

    Optional<UserGroupOperations> findById(String id);

    Page<UserGroupOperations> findAll(UserGroupFilter filter, Pageable pageable);

    void activateById(String id);

    void deleteById(String id);

    void suspendById(String id);
}
