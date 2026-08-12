package it.pagopa.selfcare.user_group.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.mongo.transitions.Mongod;
import de.flapdoodle.embed.mongo.transitions.RunningMongodProcess;
import de.flapdoodle.reverse.TransitionWalker;
import de.flapdoodle.reverse.transitions.Start;
import it.pagopa.selfcare.commons.base.security.SelfCareUser;
import it.pagopa.selfcare.user_group.SelfCareUserGroupApplication;
import it.pagopa.selfcare.user_group.api.UserGroupOperations;
import it.pagopa.selfcare.user_group.exception.ResourceNotFoundException;
import it.pagopa.selfcare.user_group.model.UserGroupEntity;
import it.pagopa.selfcare.user_group.model.UserGroupFilter;
import it.pagopa.selfcare.user_group.model.UserGroupStatus;
import it.pagopa.selfcare.user_group.security.tenant.TenantConstants;
import it.pagopa.selfcare.user_group.security.tenant.TenantId;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Exercises tenant scoping against a real embedded MongoDB.
 *
 * <p>The tenant predicate is applied in {@link UserGroupServiceImpl}'s data-access chokepoint,
 * because this Spring service mixes Spring Data repository writes with {@link MongoTemplate}
 * reads/updates. Mock-based service tests cannot catch a valid query that accidentally matches no
 * legacy documents or leaks another tenant's records.
 */
@SpringBootTest(
    classes = SelfCareUserGroupApplication.class,
    properties = "user-group.allowed.sorting.parameters=name")
class UserGroupServiceTenantIsolationTest {

  private static final int MONGO_PORT = findFreePort();
  private static TransitionWalker.ReachedState<RunningMongodProcess> mongod;

  @Autowired private UserGroupService groupService;

  @Autowired private MongoTemplate mongoTemplate;

  @DynamicPropertySource
  static void mongoProperties(DynamicPropertyRegistry registry) {
    startMongo();
    registry.add(
        "spring.data.mongodb.uri",
        () -> "mongodb://localhost:" + MONGO_PORT + "/userGroupTenantIsolationTest");
    registry.add("spring.data.mongodb.database", () -> "userGroupTenantIsolationTest");
  }

  @BeforeEach
  void setUp() {
    mongoTemplate.dropCollection(UserGroupEntity.class);
    TestSecurityContextHolder.clearContext();
    RequestContextHolder.resetRequestAttributes();
  }

  @AfterEach
  void tearDown() {
    TestSecurityContextHolder.clearContext();
    RequestContextHolder.resetRequestAttributes();
  }

  @AfterAll
  static void stopMongo() {
    if (mongod != null) {
      mongod.close();
    }
  }

  @Test
  void currentTenantCanReadOwnGroup() {
    persist(group("group-ar", "AR"));
    actAs(TenantId.AR);

    assertEquals("group-ar", groupService.getUserGroup("group-ar").getId());
  }

  @Test
  void currentTenantCannotReadAnotherTenantsGroup() {
    persist(group("group-pnpg", "PNPG"));
    actAs(TenantId.AR);

    assertThrows(ResourceNotFoundException.class, () -> groupService.getUserGroup("group-pnpg"));
  }

  @Test
  void currentTenantCanStillReadLegacyUntaggedGroup() {
    persist(group("legacy", null));
    actAs(TenantId.AR);

    assertEquals("legacy", groupService.getUserGroup("legacy").getId());
  }

  @Test
  void missingTenantContextLeavesReadUnscopedDuringMigration() {
    persist(group("group-pnpg", "PNPG"));

    assertEquals("group-pnpg", groupService.getUserGroup("group-pnpg").getId());
  }

  @Test
  void listFiltersOutOtherTenantsButKeepsLegacyGroups() {
    persist(group("group-ar", "AR"));
    persist(group("group-pnpg", "PNPG"));
    persist(group("legacy", null));
    actAs(TenantId.AR);

    List<String> ids =
        groupService.getUserGroups(new UserGroupFilter(), Pageable.unpaged())
            .map(UserGroupOperations::getId)
            .toList();

    assertEquals(2, ids.size());
    assertEquals(Set.of("group-ar", "legacy"), Set.copyOf(ids));
  }

  @Test
  void createStampsTheValidatedTenantOnNewGroups() {
    actAs(TenantId.AR);
    authenticate();
    UserGroupEntity input = group("new-group", null);

    groupService.createGroup(input);

    assertEquals("AR", mongoTemplate.findById("new-group", UserGroupEntity.class).getTenantId());
  }

  @Test
  void createOutsideRequestLeavesTenantUnset() {
    authenticate();
    UserGroupEntity input = group("new-group", null);

    groupService.createGroup(input);

    assertNull(mongoTemplate.findById("new-group", UserGroupEntity.class).getTenantId());
  }

  @Test
  void updatesCannotTouchAnotherTenantsGroup() {
    persist(group("group-pnpg", "PNPG"));
    actAs(TenantId.AR);

    assertThrows(ResourceNotFoundException.class, () -> groupService.deleteGroup("group-pnpg"));
    assertEquals(UserGroupStatus.ACTIVE, mongoTemplate.findById("group-pnpg", UserGroupEntity.class).getStatus());
  }

  private static synchronized void startMongo() {
    if (mongod == null) {
      mongod =
          Mongod.builder()
              .net(Start.to(Net.class).initializedWith(Net.of("localhost", MONGO_PORT, false)))
              .build()
              .start(Version.Main.V7_0);
    }
  }

  private static int findFreePort() {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    } catch (IOException e) {
      throw new IllegalStateException("Unable to allocate embedded MongoDB port", e);
    }
  }

  private void persist(UserGroupEntity entity) {
    mongoTemplate.save(entity);
  }

  private void actAs(TenantId tenantId) {
    var request = MockMvcRequestBuilders.get("/").buildRequest(null);
    request.setAttribute(TenantConstants.TENANT_REQUEST_ATTRIBUTE, tenantId);
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
  }

  private void authenticate() {
    SelfCareUser selfCareUser =
        SelfCareUser.builder("userId").email("test@example.com").name("name").surname("surname").build();
    TestSecurityContextHolder.setAuthentication(new TestingAuthenticationToken(selfCareUser, null));
  }

  private UserGroupEntity group(String id, String tenantId) {
    UserGroupEntity entity = new UserGroupEntity();
    entity.setId(id);
    entity.setTenantId(tenantId);
    entity.setInstitutionId("institution");
    entity.setProductId("product");
    entity.setParentInstitutionId("parent");
    entity.setName("name-" + id);
    entity.setDescription("description");
    entity.setStatus(UserGroupStatus.ACTIVE);
    entity.setMembers(Set.of("member"));
    return entity;
  }
}
