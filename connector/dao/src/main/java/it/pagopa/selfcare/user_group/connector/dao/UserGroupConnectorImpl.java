package it.pagopa.selfcare.user_group.connector.dao;

import com.mongodb.client.result.UpdateResult;
import it.pagopa.selfcare.user_group.connector.api.UserGroupConnector;
import it.pagopa.selfcare.user_group.connector.api.UserGroupOperations;
import it.pagopa.selfcare.user_group.connector.dao.model.CriteriaBuilder;
import it.pagopa.selfcare.user_group.connector.dao.model.UserGroupEntity;
import it.pagopa.selfcare.user_group.connector.exception.ResourceAlreadyExistsException;
import it.pagopa.selfcare.user_group.connector.exception.ResourceNotFoundException;
import it.pagopa.selfcare.user_group.connector.exception.ResourceUpdateException;
import it.pagopa.selfcare.user_group.connector.model.UserGroupFilter;
import it.pagopa.selfcare.user_group.connector.model.UserGroupStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.validation.ValidationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@Service
@Slf4j
public class UserGroupConnectorImpl implements UserGroupConnector {

    private final UserGroupRepository repository;
    private final MongoTemplate mongoTemplate;
    private final AuditorAware<String> auditorAware;
    private static final String COULD_NOT_UPDATE_MESSAGE = "Couldn't update resource";


    @Autowired
    public UserGroupConnectorImpl(UserGroupRepository repository, MongoTemplate mongoTemplate, AuditorAware<String> auditorAware) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
        this.auditorAware = auditorAware;
    }

    @Override
    public UserGroupOperations insert(UserGroupOperations entity) {
        log.trace("insert start");
        log.debug("insert entity = {}", entity);
        UserGroupEntity insert;
        try {
            insert = repository.insert(new UserGroupEntity(entity));
        } catch (DuplicateKeyException e) {
            throw new ResourceAlreadyExistsException("Failed _id or unique index constraint.", e);
        }

        log.trace("insert end");
        return insert;
    }

    @Override
    public UserGroupOperations save(UserGroupOperations entity) {
        log.trace("save start");
        log.debug("save entity = {}", entity);
        UserGroupEntity result;
        try {
            result = repository.save(new UserGroupEntity(entity));
        } catch (DuplicateKeyException e) {
            throw new ResourceAlreadyExistsException("Failed _id or unique index constraint.", e);
        }
        log.debug("save result = {}", result);
        log.trace("save end");
        return result;
    }

    @Override
    public void insertMember(String id, String memberId) {
        log.trace("insertMember start");
        log.debug("insertMember id = {}, memberId = {}", id, memberId);

        UpdateResult updateResult = mongoTemplate.updateFirst(
                Query.query(Criteria.where(UserGroupEntity.Fields.id).is(id)
                        .and(UserGroupEntity.Fields.status).is(UserGroupStatus.ACTIVE)),
                new Update().push(UserGroupEntity.Fields.members, memberId)
                        .set(UserGroupEntity.Fields.modifiedBy, auditorAware.getCurrentAuditor().orElse(null))
                        .currentDate(UserGroupEntity.Fields.modifiedAt),
                UserGroupEntity.class);
        if (updateResult.getModifiedCount() == 0) {
            throw new ResourceUpdateException(COULD_NOT_UPDATE_MESSAGE);
        }
        log.trace("insertMember end");

    }

    @Override
    public void deleteMember(String id, String memberId) {
        log.trace("deleteMember start");
        log.debug("deleteMember id = {}, memberId = {}", id, memberId);

        UpdateResult updateResult = mongoTemplate.updateFirst(
                Query.query(Criteria.where(UserGroupEntity.Fields.id).is(id)
                        .and(UserGroupEntity.Fields.status).is(UserGroupStatus.ACTIVE)),
                new Update().pull(UserGroupEntity.Fields.members, memberId)
                        .set(UserGroupEntity.Fields.modifiedBy, auditorAware.getCurrentAuditor().orElse(null))
                        .currentTimestamp(UserGroupEntity.Fields.modifiedAt),
                UserGroupEntity.class);
        if (updateResult.getModifiedCount() == 0) {
            throw new ResourceUpdateException(COULD_NOT_UPDATE_MESSAGE);
        }
        log.trace("deleteMember end");
    }

    public void deleteMembers(String memberId, String institutionId, String productId) {
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

    @Override
    public Optional<UserGroupOperations> findById(String id) {
        log.trace("findById start");
        log.debug("findById id = {} ", id);
        Optional<UserGroupOperations> result = repository.findById(id).map(Function.identity());
        log.debug("findById result = {}", result);
        log.trace("findById end");

        return result;
    }

    @Override
    public Page<UserGroupOperations> findAll(UserGroupFilter filter, Pageable pageable) {
        log.trace("findAll start");
        log.debug("findAll institutionId= {} , productId = {}, userId = {}, pageable = {}", filter.getInstitutionId(), filter.getProductId(), filter.getUserId(), pageable);
        if (pageable.getSort().isSorted() && !StringUtils.hasText(filter.getProductId()) && !StringUtils.hasText(filter.getInstitutionId())) {
            throw new ValidationException("Sorting not allowed without productId or institutionId");
        }
        if (filter.getStatus().size() == 1 && !StringUtils.hasText(filter.getUserId()) && !StringUtils.hasText(filter.getProductId()) && !StringUtils.hasText(filter.getInstitutionId())) {
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


    @Override
    public void activateById(String id) {
        log.trace("activateById start");
        log.debug("activateById id = {} ", id);
        updateUserById(id, UserGroupStatus.ACTIVE);
        log.trace("activateById end");

    }


    @Override
    public void deleteById(String id) {
        log.trace("deleteById start");
        log.debug("deleteById id = {} ", id);
        updateUserById(id, UserGroupStatus.DELETED);
        log.trace("deleteById end");
    }

    @Override
    public void suspendById(String id) {
        log.trace("suspendById start");
        log.debug("suspendById id = {} ", id);
        updateUserById(id, UserGroupStatus.SUSPENDED);
        log.trace("suspendById end");

    }

    private void updateUserById(String id, UserGroupStatus status) {
        log.trace("updateUserById start");
        log.debug("updateUserById id = {}, status = {}", id, status);
        UpdateResult updateResult = mongoTemplate.updateFirst(
                Query.query(Criteria.where(UserGroupEntity.Fields.id).is(id)),
                Update.update(UserGroupEntity.Fields.status, status)
                        .set(UserGroupEntity.Fields.modifiedBy, auditorAware.getCurrentAuditor().orElse(null))
                        .currentTimestamp(UserGroupEntity.Fields.modifiedAt),
                UserGroupEntity.class);
        if (updateResult.getMatchedCount() == 0) {
            throw new ResourceNotFoundException();
        }
        log.trace("updateUserById end");

    }

    private Criteria constructCriteria(UserGroupFilter filter) {
        return CriteriaBuilder.builder()
                .isIfNotNull(UserGroupEntity.Fields.institutionId, filter.getInstitutionId())
                .isIfNotNull(UserGroupEntity.Fields.productId, filter.getProductId())
                .isIfNotNull(UserGroupEntity.Fields.members, filter.getUserId())
                .inIfNotEmpty(UserGroupEntity.Fields.status, filter.getStatus())
                .build();

    }

}
