package it.pagopa.selfcare.user_group.service;

import com.mongodb.client.result.UpdateResult;
import it.pagopa.selfcare.commons.base.security.SelfCareUser;
import it.pagopa.selfcare.user_group.api.UserGroupOperations;
import it.pagopa.selfcare.user_group.dao.UserGroupRepository;
import it.pagopa.selfcare.user_group.exception.ResourceAlreadyExistsException;
import it.pagopa.selfcare.user_group.exception.ResourceNotFoundException;
import it.pagopa.selfcare.user_group.exception.ResourceUpdateException;
import it.pagopa.selfcare.user_group.model.CriteriaBuilder;
import it.pagopa.selfcare.user_group.model.UserGroupEntity;
import it.pagopa.selfcare.user_group.model.UserGroupFilter;
import it.pagopa.selfcare.user_group.model.UserGroupStatus;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import jakarta.validation.ValidationException;
import java.util.*;
import java.util.function.Function;

@Slf4j
@Service
public class UserGroupServiceImpl implements UserGroupService {

    private static final String USER_GROUP_ID_REQUIRED_MESSAGE = "A user group id is required";
    private static final String TRYING_TO_MODIFY_SUSPENDED_GROUP = "Trying to modify suspended group";
    private static final String USER_GROUP_INSTITUTION_ID_REQUIRED_MESSAGE = "A user group institution id is required";
    private static final String USER_GROUP_PARENT_INSTITUTION_ID_REQUIRED_MESSAGE = "A user group parent institution id is required";
    private static final String MEMBERS_REQUIRED = "Members are required";
    private static final String GROUP_NAME_REQUIRED = "A group name is required";
    private static final String MEMBER_ID_REQUIRED = "A member id is required";
    private static final String GROUP_NAME_FORBIDDEN = "Group name cannot start with 'Ente Aggregatore'";
    private static final String GROUP_NAME_ALREADY_EXISTS = "A group with the same name already exists in ACTIVE or SUSPENDED state";
    private static final String ENTE_AGGREGATORE_PLACEHOLDER = "Ente Aggregatore ";
    private final List<String> allowedSortingParams;
    private final UserGroupRepository repository;
    private final MongoTemplate mongoTemplate;
    private final AuditorAware<String> auditorAware;
    private static final String COULD_NOT_UPDATE_MESSAGE = "Couldn't update resource";

    @Autowired
    UserGroupServiceImpl(@Value("${user-group.allowed.sorting.parameters}") String[] allowedSortingParams,
                         UserGroupRepository repository, MongoTemplate mongoTemplate, AuditorAware<String> auditorAware) {
        this.allowedSortingParams = Arrays.asList(allowedSortingParams);
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
        this.auditorAware = auditorAware;
    }

    @Override
    public UserGroupOperations createGroup(UserGroupOperations group) {
        log.trace("createGroup start");
        log.debug("createGroup group = {}", group);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Assert.state(authentication != null, "Authentication is required");
        Assert.state(authentication.getPrincipal() instanceof SelfCareUser, "Not SelfCareUser principal");
        Assert.notNull(group, "A group is required");

        checkGroupUniqueness(group.getId(), group.getName(), group.getProductId(), group.getInstitutionId());
        return insertUserGroupEntity(group);
    }

    @Override
    public void addMember(String id, UUID memberId) {
        log.trace("addMember start");
        log.debug("addMember id = {}, memberId ={}", Encode.forJava(id), memberId);
        Assert.hasText(id, USER_GROUP_ID_REQUIRED_MESSAGE);
        Assert.notNull(memberId, MEMBER_ID_REQUIRED);
        UserGroupOperations foundGroup = findById(id).orElseThrow(ResourceNotFoundException::new);
        if (UserGroupStatus.SUSPENDED.equals(foundGroup.getStatus())) {
            throw new ResourceUpdateException(TRYING_TO_MODIFY_SUSPENDED_GROUP);
        }
        insertMember(id, memberId.toString());
        log.trace("addMember end");
    }

    @Override
    public void createGroupOrAddMembers(UserGroupOperations userGroupOperations) {
        log.trace("createGroupOrAddMembers start");
        log.debug("createGroupOrAddMembers institutionId = {}, parentInstitutionId = {}, members = {}",
                Encode.forJava(userGroupOperations.getInstitutionId()), Encode.forJava(userGroupOperations.getParentInstitutionId()), Encode.forJava(userGroupOperations.getMembers().toString()));
        Assert.notNull(userGroupOperations.getInstitutionId(), USER_GROUP_INSTITUTION_ID_REQUIRED_MESSAGE);
        Assert.notNull(userGroupOperations.getParentInstitutionId(), USER_GROUP_PARENT_INSTITUTION_ID_REQUIRED_MESSAGE);
        Assert.notNull(userGroupOperations.getMembers(), MEMBERS_REQUIRED);

        Query query = new Query(Criteria.where(UserGroupEntity.Fields.institutionId).is(userGroupOperations.getInstitutionId())
                .and(UserGroupEntity.Fields.productId).is(userGroupOperations.getProductId())
                .and(UserGroupEntity.Fields.parentInstitutionId).is(userGroupOperations.getParentInstitutionId())
                .and(UserGroupEntity.Fields.status).in(List.of(UserGroupStatus.ACTIVE, UserGroupStatus.SUSPENDED)));

        Update update = new Update()
                .setOnInsert(UserGroupEntity.Fields.name, ENTE_AGGREGATORE_PLACEHOLDER + userGroupOperations.getName())
                .setOnInsert(UserGroupEntity.Fields.description, userGroupOperations.getDescription())
                .setOnInsert(UserGroupEntity.Fields.status, UserGroupStatus.ACTIVE)
                .addToSet(UserGroupEntity.Fields.members).each(userGroupOperations.getMembers());

        FindAndModifyOptions options = FindAndModifyOptions.options()
                .upsert(true);

        mongoTemplate.findAndModify(query, update, options, UserGroupEntity.class);
        log.trace("createGroupOrAddMembers end");
    }

    @Override
    public void deleteMembersWithParentInstitutionId(String institutionId, String parentInstitutionId, String productId, Set<UUID> members) {
        log.trace("deleteMembersWithParentInstitutionId start");
        log.debug("deleteMembersWithParentInstitutionId institutionId = {}, parentInstitutionId = {}, members = {}",
                Encode.forJava(institutionId), Encode.forJava(parentInstitutionId), Encode.forJava(members.toString()));
        Assert.notNull(institutionId, USER_GROUP_INSTITUTION_ID_REQUIRED_MESSAGE);
        Assert.notNull(parentInstitutionId, USER_GROUP_PARENT_INSTITUTION_ID_REQUIRED_MESSAGE);
        Assert.notNull(members, MEMBERS_REQUIRED);
        UserGroupFilter userGroupFilter = UserGroupFilter.builder()
                .institutionId(institutionId)
                .parentInstitutionId(parentInstitutionId)
                .productId(productId)
                .build();
        String groupId = findGroupId(userGroupFilter);
        removeMembersWithParentInstitutionId(groupId, members);
        log.trace("deleteMembersWithParentInstitutionId end");
    }


    @Override
    public void deleteMember(String groupId, String memberId) {
        log.trace("deleteMember start");
        log.debug("deleteMember groupId = {}, memberId = {}", Encode.forJava(groupId), Encode.forJava(memberId));
        Assert.hasText(groupId, USER_GROUP_ID_REQUIRED_MESSAGE);
        Assert.hasText(memberId, MEMBER_ID_REQUIRED);
        UserGroupOperations foundGroup = findById(groupId).orElseThrow(ResourceNotFoundException::new);
        if (UserGroupStatus.SUSPENDED.equals(foundGroup.getStatus())) {
            throw new ResourceUpdateException(TRYING_TO_MODIFY_SUSPENDED_GROUP);
        }
        removeMemberFromActiveGroup(groupId, memberId);
        log.trace("deleteMember end");
    }

    @Override
    public void deleteMembers(String memberId, String institutionId, String productId) {
        log.trace("deleteMembers start");
        log.debug("deleteMembers memberId = {}, institutionId = {}, productId= {}", Encode.forJava(memberId), Encode.forJava(institutionId), Encode.forJava(productId));
        Assert.hasText(memberId, MEMBER_ID_REQUIRED);
        Assert.hasText(institutionId, "A institution id is required");
        Assert.hasText(productId, "A product id is required");
        removeMembers(memberId, institutionId, productId);
        log.trace("deleteMembers end");
    }

    @Override
    public UserGroupOperations getUserGroup(String id) {
        log.trace("getUserGroup start");
        log.debug("getUserGroup id = {}", id);
        Assert.hasText(id, USER_GROUP_ID_REQUIRED_MESSAGE);
        UserGroupOperations foundGroup = findById(id).orElseThrow(ResourceNotFoundException::new);
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
        Page<UserGroupOperations> result = findAll(filter, pageable);
        log.debug("getUserGroups result = {}", result);
        log.trace("getUserGroups end");
        return result;
    }

    @Override
    public void deleteGroup(String id) {
        log.trace("deleteGroup start");
        log.debug("deleteGroup id = {}", Encode.forJava(id));
        Assert.hasText(id, USER_GROUP_ID_REQUIRED_MESSAGE);
        deleteById(id);
        log.trace("deleteProduct end");
    }

    @Override
    public void suspendGroup(String id) {
        log.trace("suspendGroup start");
        log.debug("suspendGroup id = {}", Encode.forJava(id));
        Assert.hasText(id, USER_GROUP_ID_REQUIRED_MESSAGE);
        suspendById(id);
        log.trace("suspendGroup end");
    }

    @Override
    public void activateGroup(String id) {
        log.trace("activateGroup start");
        log.debug("activateGroup id = {}", Encode.forJava(id));
        Assert.hasText(id, USER_GROUP_ID_REQUIRED_MESSAGE);
        activateById(id);
        log.trace("activateGroup end");
    }

    @Override
    public UserGroupOperations updateGroup(String id, UserGroupOperations group) {
        log.trace("updateGroup start");
        log.debug("updateGroup id = {}, group = {}", Encode.forJava(id), group);
        Assert.hasText(id, USER_GROUP_ID_REQUIRED_MESSAGE);
        Assert.notNull(group, "A user group is required");
        UserGroupOperations foundGroup = findById(id).orElseThrow(ResourceNotFoundException::new);
        if (UserGroupStatus.SUSPENDED.equals(foundGroup.getStatus())) {
            throw new ResourceUpdateException(TRYING_TO_MODIFY_SUSPENDED_GROUP);
        }

        checkGroupUniqueness(id, group.getName(), foundGroup.getProductId(), foundGroup.getInstitutionId());

        foundGroup.setMembers(group.getMembers());
        foundGroup.setName(group.getName());
        foundGroup.setDescription(group.getDescription());
        UserGroupOperations updatedGroup = save(foundGroup);
        log.debug("updateGroup updatedGroup = {}", updatedGroup);
        log.trace("updateGroup end");
        return updatedGroup;
    }

    private UserGroupEntity insertUserGroupEntity(UserGroupOperations group) {
        log.trace("insert start");
        log.debug("insert entity = {}", group);
        UserGroupEntity insert;
        try {
            insert = repository.insert(new UserGroupEntity(group));
        } catch (DuplicateKeyException e) {
            throw new ResourceAlreadyExistsException("Failed _id or unique index constraint.", e);
        }
        log.debug("insert = {}", insert);
        log.trace("createGroup end");
        return insert;
    }

    private void checkGroupUniqueness(String currentGroupId, String groupName, String productId, String institutionId) {

        Assert.notNull(groupName, GROUP_NAME_REQUIRED);
        Assert.isTrue(!groupName.startsWith(ENTE_AGGREGATORE_PLACEHOLDER), GROUP_NAME_FORBIDDEN);

        Query query = new Query(
                Criteria.where(UserGroupEntity.Fields.institutionId).is(institutionId)
                        .and(UserGroupEntity.Fields.productId).is(productId)
                        .and(UserGroupEntity.Fields.status).in(List.of(UserGroupStatus.ACTIVE, UserGroupStatus.SUSPENDED))
        );

        List<UserGroupEntity> foundGroups = mongoTemplate.find(query, UserGroupEntity.class);

        boolean isSameName = foundGroups.stream().anyMatch(g ->
                g.getName().equals(groupName) && !g.getId().equals(currentGroupId));

        if (isSameName) {
            log.warn("Attempted to create/update group with duplicate name: {}", groupName);
            throw new ResourceAlreadyExistsException(GROUP_NAME_ALREADY_EXISTS);
        }    }

    private Query createActiveGroupQuery(String id) {
        return Query.query(Criteria.where(UserGroupEntity.Fields.ID).is(id)
                .and(UserGroupEntity.Fields.status).is(UserGroupStatus.ACTIVE));
    }

    private void insertMember(String id, String memberId) {
        log.trace("insertMember start");
        log.debug("insertMember id = {}, memberId = {}", id, memberId);
        UpdateResult updateResult = mongoTemplate.updateFirst(
                createActiveGroupQuery(id),
                new Update().push(UserGroupEntity.Fields.members, memberId)
                        .set(UserGroupEntity.Fields.modifiedBy, auditorAware.getCurrentAuditor().orElse(null))
                        .currentDate(UserGroupEntity.Fields.modifiedAt),
                UserGroupEntity.class);
        if (updateResult.getModifiedCount() == 0) {
            throw new ResourceUpdateException(COULD_NOT_UPDATE_MESSAGE);
        }
        log.trace("insertMember end");
    }

    private String findGroupId(UserGroupFilter userGroupFilter) {
        Page<UserGroupOperations> foundGroups = findAll(userGroupFilter, Pageable.unpaged());

        long total = foundGroups.getTotalElements();
        if (total == 0) {
            throw new ResourceNotFoundException();
        }
        if (total > 1) {
            throw new IllegalStateException("Expected a single UserGroup, but found " + total);
        }

        return foundGroups.getContent().get(0).getId();
    }

    private void removeMembersWithParentInstitutionId(String id, Set<UUID> memberIds) {
        log.trace("removeMembersWithParentInstitutionId start");
        log.debug("removeMembersWithParentInstitutionId id = {}, memberIds = {}", Encode.forJava(id), Encode.forJava(memberIds.toString()));

        Object[] userIdsAsStrings = memberIds.stream()
                .map(UUID::toString)
                .toArray();

        UpdateResult updateResult = mongoTemplate.updateFirst(
                createActiveGroupQuery(id),
                new Update()
                        .pullAll(UserGroupEntity.Fields.members, userIdsAsStrings)
                        .set(UserGroupEntity.Fields.modifiedBy, auditorAware.getCurrentAuditor().orElse(null))
                        .currentDate(UserGroupEntity.Fields.modifiedAt),
                UserGroupEntity.class);

        if (updateResult.getModifiedCount() == 0) {
            log.warn("No member to delete from UserGroup");
        }

        log.trace("removeMembersWithParentInstitutionId end");
    }


    private void removeMemberFromActiveGroup(String id, String memberId) {
        log.trace("deleteMember start");
        log.debug("deleteMember id = {}, memberId = {}", id, memberId);

        UpdateResult updateResult = mongoTemplate.updateFirst(
                createActiveGroupQuery(id),
                new Update().pull(UserGroupEntity.Fields.members, memberId)
                        .set(UserGroupEntity.Fields.modifiedBy, auditorAware.getCurrentAuditor().orElse(null))
                        .currentTimestamp(UserGroupEntity.Fields.modifiedAt),
                UserGroupEntity.class);
        if (updateResult.getModifiedCount() == 0) {
            throw new ResourceUpdateException(COULD_NOT_UPDATE_MESSAGE);
        }
        log.trace("deleteMember end");
    }

    private void removeMembers(String memberId, String institutionId, String productId) {
        log.trace("deleteMembers start");
        log.debug("deleteMembers id = {}, institutionId = {}, productId= {}", memberId, institutionId, productId);

        UpdateResult updateResult = mongoTemplate.updateMulti(
                Query.query(Criteria.where(UserGroupEntity.Fields.members).is(memberId)
                        .and(UserGroupEntity.Fields.institutionId).is(institutionId)
                        .and(UserGroupEntity.Fields.productId).is(productId)),
                new Update().pull(UserGroupEntity.Fields.members, memberId)
                        .set(UserGroupEntity.Fields.modifiedBy, auditorAware.getCurrentAuditor().orElse(null))
                        .currentTimestamp(UserGroupEntity.Fields.modifiedAt),
                UserGroupEntity.class);
        if (updateResult.getModifiedCount() == 0) {
            log.warn("No user to delete from UserGroup");
        }
        log.trace("deleteMembers end");
    }

    private Optional<UserGroupOperations> findById(String id) {
        log.trace("findById start");
        log.debug("findById id = {} ", id);
        Optional<UserGroupOperations> result = repository.findById(id).map(Function.identity());
        log.debug("findById result = {}", result);
        log.trace("findById end");

        return result;
    }

    private Page<UserGroupOperations> findAll(UserGroupFilter filter, Pageable pageable) {
        log.trace("findAll start");
        log.debug("findAll institutionId = {}, parentInstitutionId = {}, productId = {}, userId = {}, pageable = {}",
                Encode.forJava(filter.getInstitutionId()), Encode.forJava(filter.getParentInstitutionId()),
                Encode.forJava(filter.getProductId()), Encode.forJava(filter.getUserId()), pageable);
        if (pageable.getSort().isSorted() && !StringUtils.hasText(filter.getProductId()) && !StringUtils.hasText(filter.getInstitutionId())) {
            throw new ValidationException("Sorting not allowed without productId or institutionId");
        }
        if (Optional.ofNullable(filter.getStatus()).map(List::size).orElse(0) == 1 && !StringUtils.hasText(filter.getUserId()) && !StringUtils.hasText(filter.getProductId()) && !StringUtils.hasText(filter.getInstitutionId())) {
            throw new ValidationException("At least one of productId, institutionId and userId must be provided with status filter");
        }
        Query query = new Query(constructCriteria(filter));
        long count = this.mongoTemplate.count(query, UserGroupEntity.class);
        List<UserGroupOperations> userGroupOperations = new ArrayList<>(mongoTemplate.find(query.with(pageable), UserGroupEntity.class));
        final Page<UserGroupOperations> result = PageableExecutionUtils.getPage(userGroupOperations, pageable, () -> count);
        log.debug("findAll result = {}", result);
        log.trace("findAll end");
        return result;
    }

    private Criteria constructCriteria(UserGroupFilter filter) {
        return CriteriaBuilder.builder()
                .isIfNotNull(UserGroupEntity.Fields.institutionId, filter.getInstitutionId())
                .isIfNotNull(UserGroupEntity.Fields.productId, filter.getProductId())
                .isIfNotNull(UserGroupEntity.Fields.members, filter.getUserId())
                .inIfNotEmpty(UserGroupEntity.Fields.status, filter.getStatus())
                .isIfNotNull(UserGroupEntity.Fields.parentInstitutionId, filter.getParentInstitutionId())
                .build();

    }

    private void activateById(String id) {
        log.trace("activateById start");
        log.debug("activateById id = {} ", id);
        updateUserById(id, UserGroupStatus.ACTIVE);
        log.trace("activateById end");

    }

    private void deleteById(String id) {
        log.trace("deleteById start");
        log.debug("deleteById id = {} ", id);
        updateUserById(id, UserGroupStatus.DELETED);
        log.trace("deleteById end");
    }

    private void suspendById(String id) {
        log.trace("suspendById start");
        log.debug("suspendById id = {} ", id);
        updateUserById(id, UserGroupStatus.SUSPENDED);
        log.trace("suspendById end");
    }

    private void updateUserById(String id, UserGroupStatus status) {
        log.trace("updateUserById start");
        log.debug("updateUserById id = {}, status = {}", id, status);
        UpdateResult updateResult = mongoTemplate.updateFirst(
                Query.query(Criteria.where(UserGroupEntity.Fields.ID).is(id)),
                Update.update(UserGroupEntity.Fields.status, status)
                        .set(UserGroupEntity.Fields.modifiedBy, auditorAware.getCurrentAuditor().orElse(null))
                        .currentTimestamp(UserGroupEntity.Fields.modifiedAt),
                UserGroupEntity.class);
        if (updateResult.getMatchedCount() == 0) {
            throw new ResourceNotFoundException();
        }
        log.trace("updateUserById end");
    }

    private UserGroupOperations save(UserGroupOperations group) {
        log.trace("save start");
        log.debug("save entity = {}", group);
        UserGroupEntity result;
        try {
            result = repository.save(new UserGroupEntity(group));
        } catch (DuplicateKeyException e) {
            throw new ResourceAlreadyExistsException("Failed _id or unique index constraint.", e);
        }
        log.debug("save result = {}", result);
        log.trace("save end");
        return result;
    }
}