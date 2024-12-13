package it.pagopa.selfcare.user_group.dao;

import com.mongodb.client.result.UpdateResult;
import it.pagopa.selfcare.user_group.config.CoreConfig;
import it.pagopa.selfcare.user_group.model.UserGroupEntity;
import it.pagopa.selfcare.user_group.model.UserGroupFilter;
import it.pagopa.selfcare.user_group.model.UserGroupStatus;
import it.pagopa.selfcare.commons.base.security.SelfCareUser;
import it.pagopa.selfcare.commons.utils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;

import javax.validation.ValidationException;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DataMongoTest
@EnableAutoConfiguration
@ContextConfiguration(classes = {UserGroupEntity.class, UserGroupRepository.class, CoreConfig.class})
class UserGroupRepositoryTest {

    @Autowired
    private UserGroupRepository repository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private AuditorAware<String> auditorAware;

    @AfterEach
    void clear() {
        repository.deleteAll();
    }


    @Test
    void create() {
        //given
        SelfCareUser selfCareUser = SelfCareUser.builder("id")
                .email("test@example.com")
                .name("name")
                .surname("surname")
                .build();
        TestingAuthenticationToken authenticationToken = new TestingAuthenticationToken(selfCareUser, null);
        TestSecurityContextHolder.setAuthentication(authenticationToken);
        UserGroupEntity group = TestUtils.mockInstance(new UserGroupEntity(), "setId",
                "setCreatedAt",
                "setCreatedBy",
                "setModifiedAt",
                "setModifiedBy");
        group.setCreatedBy(selfCareUser.getId());
        //when
        UserGroupEntity savedGroup = repository.insert(group);
        //then
        assertEquals(selfCareUser.getId(), savedGroup.getCreatedBy());
        assertNull(savedGroup.getModifiedBy());
        assertNotNull(savedGroup, "id cannot be null after entity creation");
    }


    @Test
    void delete() {
        //given
        Instant now = Instant.now().minusSeconds(1);
        SelfCareUser selfCareUser = SelfCareUser.builder("id")
                .email("test@example.com")
                .name("name")
                .surname("surname")
                .build();
        TestingAuthenticationToken authenticationToken = new TestingAuthenticationToken(selfCareUser, null);
        TestSecurityContextHolder.setAuthentication(authenticationToken);
        UserGroupEntity group = TestUtils.mockInstance(new UserGroupEntity(), "setId",
                "setCreatedAt",
                "setCreateBy",
                "setModifiedAt",
                "setModifiedBy");
        UserGroupEntity savedGroup = repository.insert(group);
        Optional<UserGroupEntity> found = repository.findById(savedGroup.getId());
        //when
        mongoTemplate.remove(Query.query(Criteria.where("_id").is(savedGroup.getId())), UserGroupEntity.class);
        //then
        Optional<UserGroupEntity> deleted = repository.findById(savedGroup.getId());
        assertEquals(Optional.empty(), deleted);

    }

    @Test
    void update() {
        //given
        Instant now = Instant.now().minusSeconds(1);
        SelfCareUser selfCareUser = SelfCareUser.builder("id")
                .email("test@example.com")
                .name("name")
                .surname("surname")
                .build();
        TestingAuthenticationToken authenticationToken = new TestingAuthenticationToken(selfCareUser, null);
        TestSecurityContextHolder.setAuthentication(authenticationToken);
        UserGroupEntity group = TestUtils.mockInstance(new UserGroupEntity(), "setId",
                "setCreatedAt",
                "setCreatedBy",
                "setModifiedAt",
                "setModifiedBy");
        group.setCreatedAt(now);
        UserGroupEntity savedGroup = repository.insert(group);

        Optional<UserGroupEntity> groupMod = repository.findById(savedGroup.getId());
        groupMod.get().setId(savedGroup.getId());
        groupMod.get().setModifiedBy(selfCareUser.getId());
        groupMod.get().setModifiedAt(Instant.now());
        groupMod.get().setStatus(UserGroupStatus.SUSPENDED);
        //when
        UserGroupEntity modifiedGroup = repository.save(groupMod.get());
        //then
        assertNull(savedGroup.getModifiedBy());
        assertEquals(selfCareUser.getId(), modifiedGroup.getModifiedBy());
        assertTrue(modifiedGroup.getModifiedAt().isAfter(savedGroup.getCreatedAt()));
        assertEquals(UserGroupStatus.ACTIVE, savedGroup.getStatus());
        assertEquals(UserGroupStatus.SUSPENDED, modifiedGroup.getStatus());
    }

    @Test
    void addMember() {
        //given
        Instant now = Instant.now().minusSeconds(1);
        SelfCareUser selfCareUser = SelfCareUser.builder("id")
                .email("test@example.com")
                .name("name")
                .surname("surname")
                .build();
        TestingAuthenticationToken authenticationToken = new TestingAuthenticationToken(selfCareUser, null);
        TestSecurityContextHolder.setAuthentication(authenticationToken);
        UserGroupEntity group = TestUtils.mockInstance(new UserGroupEntity(), "setId",
                "setCreatedAt",
                "setCreatedBy",
                "setModifiedAt",
                "setModifiedBy");
        group.setCreatedBy(selfCareUser.getId());
        UserGroupEntity savedGroup = repository.insert(group);
        UUID memberUID = UUID.randomUUID();
        //when
        UpdateResult updateResult1 = mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(savedGroup.getId())),
                new Update().push("members", memberUID.toString()),
                UserGroupEntity.class);
        UpdateResult updateResult2 = mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(savedGroup.getId())),
                new Update().push("members", UUID.randomUUID()),
                UserGroupEntity.class);
        UpdateResult updateResult3 = mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(savedGroup.getId())),
                new Update().push("members", memberUID.toString()),
                UserGroupEntity.class);
        //then
        Optional<UserGroupEntity> groupMod = repository.findById(savedGroup.getId());
        assertEquals(selfCareUser.getId(), savedGroup.getCreatedBy());
        assertEquals(1, updateResult1.getMatchedCount());
        assertEquals(1, updateResult2.getMatchedCount());
        assertEquals(1, updateResult3.getMatchedCount());
        assertEquals(2, groupMod.get().getMembers().size());
    }

    @Test
    void suspend() {
        Instant now = Instant.now().minusSeconds(1);
        SelfCareUser selfCareUser = SelfCareUser.builder("id")
                .email("test@example.com")
                .name("name")
                .surname("surname")
                .build();
        TestingAuthenticationToken authenticationToken = new TestingAuthenticationToken(selfCareUser, null);
        TestSecurityContextHolder.setAuthentication(authenticationToken);
        UserGroupEntity group = TestUtils.mockInstance(new UserGroupEntity(), "setId",
                "setCreatedAt",
                "setCreatedBy",
                "setModifiedAt",
                "setModifiedBy");
        group.setCreatedBy(selfCareUser.getId());
        group.setModifiedBy(selfCareUser.getId());
        UserGroupEntity savedGroup = repository.insert(group);
         mongoTemplate.updateFirst(
                Query.query(Criteria.where(UserGroupEntity.Fields.ID).is(savedGroup.getId())),
                Update.update(UserGroupEntity.Fields.status, UserGroupStatus.SUSPENDED)
                        .set("modifiedBy", auditorAware.getCurrentAuditor().get())
                        .set("modifiedAt", now),
                UserGroupEntity.class);
        //then
        Optional<UserGroupEntity> groupMod = repository.findById(savedGroup.getId());
        assertEquals(selfCareUser.getId(), groupMod.get().getModifiedBy());
        assertEquals(selfCareUser.getId(), savedGroup.getCreatedBy());
    }

    @Test
    void findAll() {
        //given
        Instant now = Instant.now().minusSeconds(1);
        SelfCareUser selfCareUser = SelfCareUser.builder("id")
                .email("test@example.com")
                .name("name")
                .surname("surname")
                .build();
        TestingAuthenticationToken authenticationToken = new TestingAuthenticationToken(selfCareUser, null);
        TestSecurityContextHolder.setAuthentication(authenticationToken);
        UserGroupEntity group1 = TestUtils.mockInstance(new UserGroupEntity(), "setId",
                "setCreatedAt",
                "setCreateBy",
                "setModifiedAt",
                "setModifiedBy");
        String institutionId = "institutionId";
        String productId = "productId";
        String userId = "userId";
        group1.setProductId(productId);
        group1.setName("alfa");
        group1.setMembers(Set.of("userId", "userId2"));
        group1.setInstitutionId(institutionId);
        UserGroupEntity savedGroup = repository.insert(group1);
        UserGroupEntity group2 = TestUtils.mockInstance(new UserGroupEntity(), "setId",
                "setCreatedAt",
                "setCreateBy",
                "setModifiedAt",
                "setModifiedBy");
        group2.setProductId(productId);
        group2.setName("beta");
        group2.setInstitutionId(institutionId);
        group2.setMembers(Set.of("userId"));
        UserGroupEntity savedGroup1 = repository.insert(group2);

        Pageable pageable = PageRequest.of(0, 3);
        UserGroupFilter filter = new UserGroupFilter(institutionId, productId, userId, Collections.emptyList());

        Query query = new Query();
        if (pageable.getSort().isSorted() && filter.getProductId().isEmpty() && filter.getInstitutionId().isEmpty()) {
            throw new ValidationException();
        }
        query.addCriteria(Criteria.where(UserGroupEntity.Fields.institutionId).is(institutionId));
        query.addCriteria(Criteria.where(UserGroupEntity.Fields.productId).is(productId));
        query.addCriteria(Criteria.where(UserGroupEntity.Fields.members).is(userId));
        //when
        List<UserGroupEntity> foundGroups = mongoTemplate.find(query.with(pageable), UserGroupEntity.class);
        //then
        assertEquals(2, foundGroups.size());
    }

    @Test
    void findAll_allowedState() {
        //given
        Instant now = Instant.now().minusSeconds(1);
        String institutionId = "institutionId";
        String productId = "productId";
        String userId = "userId";
        List<UserGroupStatus> allowedStatus = List.of(UserGroupStatus.ACTIVE);
        SelfCareUser selfCareUser = SelfCareUser.builder("id")
                .email("test@example.com")
                .name("name")
                .surname("surname")
                .build();
        TestingAuthenticationToken authenticationToken = new TestingAuthenticationToken(selfCareUser, null);
        TestSecurityContextHolder.setAuthentication(authenticationToken);
        UserGroupEntity group1 = TestUtils.mockInstance(new UserGroupEntity(), "setId",
                "setCreatedAt",
                "setCreateBy",
                "setModifiedAt",
                "setModifiedBy");
        group1.setProductId(productId);
        group1.setName("alfa");
        group1.setMembers(Set.of("userId", "userId2"));
        group1.setInstitutionId(institutionId);
        group1.setStatus(UserGroupStatus.SUSPENDED);
        UserGroupEntity savedGroup = repository.insert(group1);
        UserGroupEntity group2 = TestUtils.mockInstance(new UserGroupEntity(), "setId",
                "setCreatedAt",
                "setCreateBy",
                "setModifiedAt",
                "setModifiedBy");
        group2.setProductId(productId);
        group2.setName("beta");
        group2.setInstitutionId(institutionId);
        group2.setMembers(Set.of("userId"));
        UserGroupEntity savedGroup1 = repository.insert(group2);
        UserGroupFilter filter = new UserGroupFilter(institutionId, productId, userId, allowedStatus);
        Pageable pageable = PageRequest.of(0, 3);
        Query query = new Query();
        if (pageable.getSort().isSorted() && filter.getProductId().isEmpty() && filter.getInstitutionId().isEmpty()) {
            throw new ValidationException();
        }
        query.addCriteria(Criteria.where(UserGroupEntity.Fields.institutionId).is(institutionId));
        query.addCriteria(Criteria.where(UserGroupEntity.Fields.productId).is(productId));
        query.addCriteria(Criteria.where(UserGroupEntity.Fields.members).is(userId));
        query.addCriteria(Criteria.where(UserGroupEntity.Fields.status).in(allowedStatus));
        //when
        List<UserGroupEntity> foundGroups = mongoTemplate.find(query.with(pageable), UserGroupEntity.class);
        //then
        assertEquals(1, foundGroups.size());
    }

    @Test
    void deleteMembers() {
        //given
        Instant now = Instant.now().minusSeconds(1);
        SelfCareUser selfCareUser = SelfCareUser.builder("id")
                .email("test@example.com")
                .name("name")
                .surname("surname")
                .build();
        TestingAuthenticationToken authenticationToken = new TestingAuthenticationToken(selfCareUser, null);
        TestSecurityContextHolder.setAuthentication(authenticationToken);
        UserGroupEntity group1 = TestUtils.mockInstance(new UserGroupEntity(), "setId",
                "setCreatedAt",
                "setCreateBy",
                "setModifiedAt",
                "setModifiedBy");
        String productId = "productId";
        String institutionId = "institutionId";
        group1.setProductId(productId);
        group1.setName("alfa");
        group1.setMembers(Set.of("userId", "userId2"));
        group1.setInstitutionId(institutionId);
        UserGroupEntity savedGroup = repository.insert(group1);
        UserGroupEntity group2 = TestUtils.mockInstance(new UserGroupEntity(), "setId",
                "setCreatedAt",
                "setCreateBy",
                "setModifiedAt",
                "setModifiedBy");
        group2.setProductId(productId);
        group2.setName("beta");
        group2.setInstitutionId(institutionId);
        group2.setMembers(Set.of("userId"));
        UserGroupEntity savedGroup1 = repository.insert(group2);
        Pageable pageable = PageRequest.of(0, 3);
        UserGroupFilter filter = new UserGroupFilter();
        String userId = "userId";
        filter.setInstitutionId(institutionId);
        filter.setProductId(productId);
        Query query = new Query();
        if (pageable.getSort().isSorted() && filter.getProductId().isEmpty() && filter.getInstitutionId().isEmpty()) {
            throw new ValidationException();
        }

        //when
        UpdateResult updateResult = mongoTemplate.updateMulti(
                Query.query(Criteria.where(UserGroupEntity.Fields.members).is("userId2")
                        .and(UserGroupEntity.Fields.institutionId).is(institutionId)
                        .and(UserGroupEntity.Fields.productId).is(productId)),
                new Update().pull("members", "userId2")
                        .set("modifiedBy", auditorAware.getCurrentAuditor().get())
                        .set("modifiedAt", now),
                UserGroupEntity.class);

        //then
        query.addCriteria(Criteria.where(UserGroupEntity.Fields.institutionId).is(institutionId));
        query.addCriteria(Criteria.where(UserGroupEntity.Fields.productId).is(productId));
        List<UserGroupEntity> foundGroups = mongoTemplate.find(query.with(pageable), UserGroupEntity.class);

        assertEquals(1, foundGroups.get(1).getMembers().size());
        assertEquals(1, foundGroups.get(0).getMembers().size());
        assertEquals(1, updateResult.getModifiedCount());
    }


}