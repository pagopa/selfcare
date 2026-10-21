package it.pagopa.selfcare.mscore.tenant;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.commons.tenant.TenantContext;
import it.pagopa.selfcare.commons.tenant.TenantRegistry;
import it.pagopa.selfcare.commons.tenant.mongodb.TenantMongoDatabaseFactory;
import it.pagopa.selfcare.mscore.connector.dao.DelegationConnectorImpl;
import it.pagopa.selfcare.mscore.connector.dao.DelegationRepository;
import it.pagopa.selfcare.mscore.connector.dao.MongoCustomConnectorImpl;
import it.pagopa.selfcare.mscore.connector.dao.MailNotificationRepository;
import it.pagopa.selfcare.mscore.connector.dao.MailNotificationConnectorImpl;
import it.pagopa.selfcare.mscore.connector.dao.model.MailNotificationEntity;
import it.pagopa.selfcare.mscore.connector.dao.TenantAwareEntityWriter;
import it.pagopa.selfcare.mscore.connector.dao.TenantOwnershipPolicy;
import it.pagopa.selfcare.mscore.connector.dao.model.DelegationEntity;
import it.pagopa.selfcare.mscore.connector.dao.model.DelegationPageEntity;
import it.pagopa.selfcare.mscore.connector.dao.model.InstitutionEntity;
import it.pagopa.selfcare.mscore.connector.dao.model.mapper.DelegationEntityMapperImpl;
import it.pagopa.selfcare.mscore.constant.DelegationType;
import it.pagopa.selfcare.mscore.model.delegation.DelegationCount;
import it.pagopa.selfcare.mscore.model.delegation.DelegationInstitution;
import it.pagopa.selfcare.mscore.model.delegation.DelegationWithPagination;
import it.pagopa.selfcare.mscore.model.delegation.GetDelegationParameters;
import it.pagopa.selfcare.mscore.model.aggregation.UserInstitutionAggregation;
import it.pagopa.selfcare.mscore.model.aggregation.UserInstitutionFilter;
import it.pagopa.selfcare.mscore.model.institution.Institution;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.UpdateDefinition;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.core.convert.converter.Converter;
import org.testcontainers.containers.GenericContainer;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static it.pagopa.selfcare.mscore.constant.DelegationState.ACTIVE;
import static it.pagopa.selfcare.mscore.constant.Order.ASC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TenantDelegationAggregationIntegrationTest {

    private static final GenericContainer<?> MONGO =
            new GenericContainer<>("mongo:7.0").withExposedPorts(27017);

    @BeforeAll
    static void startMongo() {
        MONGO.start();
    }

    @AfterAll
    static void stopMongo() {
        MONGO.stop();
    }

    @Test
    void aggregatesFilterOwnedAndLegacyDocumentsBeforeCountingPagingAndGrouping() throws Exception {
        TenantContext tenantContext = new TenantContext();
        tenantContext.setTenantId("AR");
        TenantRegistry registry = new TenantRegistry(
                new ObjectMapper(),
                new MockEnvironment().withProperty(
                        "MONGO_AR",
                        "mongodb://" + MONGO.getHost() + ":" + MONGO.getMappedPort(27017)),
                """
                {"AR":{"mongo":{"account":"local","database":"tenant_aggregation_test","connectionStringEnvVar":"MONGO_AR"}}}
                """,
                "AR",
                "");
        registry.initialize();
        TenantMongoDatabaseFactory databaseFactory =
                new TenantMongoDatabaseFactory(registry, tenantContext);
        MongoTemplate mongoTemplate = new MongoTemplate(databaseFactory, mappingConverter());
        mongoTemplate.dropCollection(DelegationEntity.class);

        List<DelegationEntity> fixtures = List.of(
                delegation("owned-1", "AR", "prod-a", "2025-01-01T00:00:00Z"),
                delegation("owned-2", "AR", "prod-a", "2025-01-02T00:00:00Z"),
                delegation("foreign", "PNPG", "prod-b", "2025-01-05T00:00:00Z"));
        for (DelegationEntity fixture : fixtures) {
            mongoTemplate.insert(fixture);
        }
        mongoTemplate.getCollection(DelegationConnectorImpl.COLLECTION_DELEGATIONS)
                .insertOne(new org.bson.Document("_id", "legacy-absent")
                        .append("productId", "prod-a")
                        .append("status", "ACTIVE")
                        .append("createdAt", "2025-01-03T00:00:00Z"));
        mongoTemplate.getCollection(DelegationConnectorImpl.COLLECTION_DELEGATIONS)
                .insertOne(new org.bson.Document("_id", "legacy-null")
                        .append("tenantId", null)
                        .append("productId", "prod-b")
                        .append("status", "ACTIVE")
                        .append("createdAt", "2025-01-04T00:00:00Z"));

        for (boolean strict : List.of(true, false)) {
            TenantOwnershipPolicy policy = new TenantOwnershipPolicy(tenantContext, strict);
            DelegationConnectorImpl connector = new DelegationConnectorImpl(
                    mock(DelegationRepository.class),
                    new DelegationEntityMapperImpl(),
                    mongoTemplate,
                    mock(TenantAwareEntityWriter.class),
                    policy);

            DelegationWithPagination page = connector.findFromDate(
                    OffsetDateTime.parse("2025-01-01T00:00:00Z"), 0L, 2L);
            long expectedTotal = strict ? 2L : 4L;
            assertEquals(expectedTotal, page.getPageInfo().getTotalElements());
            assertEquals(2, page.getDelegations().size());
            assertEquals("owned-1", page.getDelegations().get(0).getId());
            assertEquals("owned-2", page.getDelegations().get(1).getId());

            List<DelegationCount> grouped = connector.countDelegations(
                    OffsetDateTime.parse("2025-01-01T00:00:00Z"),
                    OffsetDateTime.parse("2025-01-06T00:00:00Z"),
                    null);
            assertEquals(strict ? 1 : 2, grouped.size());
            assertEquals(strict ? 2L : 3L, countFor(grouped, "prod-a"));
            if (!strict) {
                assertEquals(1L, countFor(grouped, "prod-b"));
            }
            DelegationRepository repository = mock(DelegationRepository.class);
            when(repository.find(any(), any(), eq(DelegationEntity.class)))
                    .thenReturn(new PageImpl<>(fixtures.subList(0, 2)));
            DelegationConnectorImpl countConnector = new DelegationConnectorImpl(
                    repository,
                    new DelegationEntityMapperImpl(),
                    mongoTemplate,
                    mock(TenantAwareEntityWriter.class),
                    policy);
            DelegationWithPagination countedPage = countConnector.findAndCount(
                    GetDelegationParameters.builder()
                            .from(null)
                            .to(null)
                            .productId(null)
                            .search(null)
                            .taxCode(null)
                            .order(ASC)
                            .page(0)
                            .size(2)
                            .build());
            assertEquals(expectedTotal, countedPage.getPageInfo().getTotalElements());
        }
    }

    @Test
    void lookupsFilterInstitutionOwnershipBeforeLimitInBothIsolationModes() throws Exception {
        TenantContext tenantContext = new TenantContext();
        tenantContext.setTenantId("AR");
        TenantRegistry registry = new TenantRegistry(
                new ObjectMapper(),
                new MockEnvironment().withProperty(
                        "MONGO_AR",
                        "mongodb://" + MONGO.getHost() + ":" + MONGO.getMappedPort(27017)),
                """
                {"AR":{"mongo":{"account":"local","database":"tenant_lookup_test","connectionStringEnvVar":"MONGO_AR"}}}
                """,
                "AR",
                "");
        registry.initialize();
        MongoTemplate mongoTemplate = new MongoTemplate(
                new TenantMongoDatabaseFactory(registry, tenantContext),
                mappingConverter());
        mongoTemplate.dropCollection(DelegationEntity.class);
        mongoTemplate.dropCollection(InstitutionEntity.class);

        mongoTemplate.insert(institution("owned-source", "AR"));
        mongoTemplate.insert(institution("owned-target", "AR"));
        mongoTemplate.insert(institution("foreign-source", "PNPG"));
        mongoTemplate.insert(institution("foreign-target", "PNPG"));
        for (String side : List.of("source", "target")) {
            mongoTemplate.getCollection(DelegationConnectorImpl.COLLECTION_INSTITUTION)
                    .insertMany(List.of(
                            new org.bson.Document("_id", "legacy-absent-" + side),
                            new org.bson.Document("_id", "legacy-null-" + side).append("tenantId", null)));
        }

        mongoTemplate.insert(delegationWithReferences(
                "missing-source", "absent-source", "delegator-target", "2025-01-01T00:00:00Z"));
        mongoTemplate.insert(delegationWithReferences(
                "foreign-source-root", "foreign-source", "delegator-target", "2025-01-02T00:00:00Z"));
        mongoTemplate.insert(delegationWithReferences(
                "legacy-absent-source-root", "legacy-absent-source", "delegator-target", "2025-01-03T00:00:00Z"));
        mongoTemplate.insert(delegationWithReferences(
                "legacy-null-source-root", "legacy-null-source", "delegator-target", "2025-01-04T00:00:00Z"));
        mongoTemplate.insert(delegationWithReferences(
                "owned-source-root", "owned-source", "delegator-target", "2025-01-05T00:00:00Z"));
        mongoTemplate.insert(delegationWithReferences(
                "missing-target", "delegate-source", "absent-target", "2025-01-01T00:00:00Z"));
        mongoTemplate.insert(delegationWithReferences(
                "foreign-target-root", "delegate-source", "foreign-target", "2025-01-02T00:00:00Z"));
        mongoTemplate.insert(delegationWithReferences(
                "legacy-absent-target-root", "delegate-source", "legacy-absent-target", "2025-01-03T00:00:00Z"));
        mongoTemplate.insert(delegationWithReferences(
                "legacy-null-target-root", "delegate-source", "legacy-null-target", "2025-01-04T00:00:00Z"));
        mongoTemplate.insert(delegationWithReferences(
                "owned-target-root", "delegate-source", "owned-target", "2025-01-05T00:00:00Z"));
        mongoTemplate.insert(delegationWithReferences(
                "no-match-root", "absent-source", "no-match-target", "2025-01-04T00:00:00Z"));
        mongoTemplate.insert(delegationWithReferences(
                "foreign-only-root", "foreign-source", "foreign-only-target", "2025-01-05T00:00:00Z"));
        DelegationEntity foreignSourceDelegation = delegationWithReferences(
                "foreign-delegation-source", "owned-source", "delegator-target", "2025-01-01T00:00:00Z");
        foreignSourceDelegation.setTenantId("PNPG");
        mongoTemplate.insert(foreignSourceDelegation);
        DelegationEntity foreignTargetDelegation = delegationWithReferences(
                "foreign-delegation-target", "delegate-source", "owned-target", "2025-01-01T00:00:00Z");
        foreignTargetDelegation.setTenantId("PNPG");
        mongoTemplate.insert(foreignTargetDelegation);

        for (boolean strict : List.of(true, false)) {
            DelegationConnectorImpl connector = new DelegationConnectorImpl(
                    mock(DelegationRepository.class),
                    new DelegationEntityMapperImpl(),
                    mongoTemplate,
                    mock(TenantAwareEntityWriter.class),
                    new TenantOwnershipPolicy(tenantContext, strict));

            List<DelegationInstitution> delegators = connector.findDelegators(
                    "delegator-target", "prod-a", DelegationType.EA, null, 1);
            assertEquals(1, delegators.size());
            String firstSource = strict ? "owned-source" : "legacy-absent-source";
            assertEquals(firstSource + "-root", delegators.get(0).getDelegationId());
            assertEquals(firstSource, delegators.get(0).getInstitution().getId());

            List<DelegationInstitution> delegates = connector.findDelegates(
                    "delegate-source", "prod-a", DelegationType.EA, null, 1);
            assertEquals(1, delegates.size());
            String firstTarget = strict ? "owned-target" : "legacy-absent-target";
            assertEquals(firstTarget + "-root", delegates.get(0).getDelegationId());
            assertEquals(firstTarget, delegates.get(0).getInstitution().getId());

            assertEquals(strict
                            ? List.of("owned-source-root")
                            : List.of("legacy-absent-source-root", "legacy-null-source-root", "owned-source-root"),
                    connector.findDelegators("delegator-target", "prod-a", DelegationType.EA, null, 10)
                            .stream().map(DelegationInstitution::getDelegationId).toList());
            assertEquals(strict
                            ? List.of("owned-target-root")
                            : List.of("legacy-absent-target-root", "legacy-null-target-root", "owned-target-root"),
                    connector.findDelegates("delegate-source", "prod-a", DelegationType.EA, null, 10)
                            .stream().map(DelegationInstitution::getDelegationId).toList());
            assertEquals(strict ? List.of() : List.of("legacy-null-source-root"),
                    connector.findDelegators(
                                    "delegator-target", "prod-a", DelegationType.EA, delegators.get(0).getId(), 1)
                            .stream().map(DelegationInstitution::getDelegationId).toList());
            assertEquals(strict ? List.of() : List.of("legacy-null-target-root"),
                    connector.findDelegates(
                                    "delegate-source", "prod-a", DelegationType.EA, delegates.get(0).getId(), 1)
                            .stream().map(DelegationInstitution::getDelegationId).toList());
            assertEquals(List.of(), connector.findDelegators(
                    "no-match-target", "prod-a", DelegationType.EA, null, 1));
            assertEquals(List.of(), connector.findDelegators(
                    "foreign-only-target", "prod-a", DelegationType.EA, null, 1));
        }
    }

    @Test
    void graphLookupFiltersForeignAndLegacyInstitutionsByIsolationMode() throws Exception {
        TenantContext tenantContext = new TenantContext();
        tenantContext.setTenantId("AR");
        TenantRegistry registry = new TenantRegistry(
                new ObjectMapper(),
                new MockEnvironment().withProperty(
                        "MONGO_AR",
                        "mongodb://" + MONGO.getHost() + ":" + MONGO.getMappedPort(27017)),
                """
                {"AR":{"mongo":{"account":"local","database":"tenant_graph_test","connectionStringEnvVar":"MONGO_AR"}}}
                """,
                "AR",
                "");
        registry.initialize();
        MongoTemplate mongoTemplate = new MongoTemplate(
                new TenantMongoDatabaseFactory(registry, tenantContext),
                mappingConverter());
        mongoTemplate.dropCollection("User");
        mongoTemplate.dropCollection(InstitutionEntity.class);
        mongoTemplate.getCollection("User").insertOne(new org.bson.Document("_id", "user-owned")
                .append("tenantId", "AR")
                .append("bindings", List.of(
                        new org.bson.Document("institutionId", "root-absent")
                                .append("products", new org.bson.Document("status", "ACTIVE")),
                        new org.bson.Document("institutionId", "root-null")
                                .append("products", new org.bson.Document("status", "ACTIVE")),
                        new org.bson.Document("institutionId", "legacy-absent")
                                .append("products", new org.bson.Document("status", "ACTIVE")),
                        new org.bson.Document("institutionId", "legacy-null")
                                .append("products", new org.bson.Document("status", "ACTIVE")))));
        var institutions = mongoTemplate.getCollection("Institution");
        institutions.insertOne(new org.bson.Document("_id", "root-absent")
                .append("tenantId", "AR")
                .append("externalId", "root-absent")
                .append("institutionId", "legacy-absent")
                .append("description", "owned"));
        institutions.insertOne(new org.bson.Document("_id", "legacy-absent")
                .append("externalId", "legacy-absent")
                .append("description", "legacy-absent"));
        institutions.insertOne(new org.bson.Document("_id", "foreign-absent")
                .append("tenantId", "PNPG")
                .append("externalId", "foreign-absent")
                .append("description", "foreign"));
        institutions.insertOne(new org.bson.Document("_id", "root-null")
                .append("tenantId", "AR")
                .append("externalId", "root-null")
                .append("institutionId", "legacy-null")
                .append("description", "owned"));
        institutions.insertOne(new org.bson.Document("_id", "legacy-null")
                .append("tenantId", null)
                .append("externalId", "legacy-null")
                .append("description", "legacy-null"));
        institutions.insertOne(new org.bson.Document("_id", "foreign-null")
                .append("tenantId", "PNPG")
                .append("externalId", "foreign-null")
                .append("description", "foreign"));

        UserInstitutionFilter filter = new UserInstitutionFilter();
        filter.setUserId("user-owned");
        filter.setStates(List.of("ACTIVE"));
        for (boolean strict : List.of(true, false)) {
            MongoCustomConnectorImpl connector = new MongoCustomConnectorImpl(
                    mongoTemplate,
                    new TenantOwnershipPolicy(tenantContext, strict));
            filter.setExternalId(null);
            List<UserInstitutionAggregation> modeResults =
                    connector.findUserInstitutionAggregation(filter, UserInstitutionAggregation.class);
            assertEquals(2, modeResults.size());
            List<String> expectedIds = strict
                    ? List.of("root-absent", "root-null")
                    : List.of("legacy-absent", "legacy-null");
            assertEquals(expectedIds, modeResults.stream()
                    .map(result -> result.getInstitutions().get(0).getId())
                    .sorted()
                    .toList());

            filter.setExternalId("root-absent");
            List<UserInstitutionAggregation> results = connector.findUserInstitutionAggregation(
                    filter, UserInstitutionAggregation.class);
            assertEquals(1, results.size());
            assertEquals(List.of("root-absent"), results.get(0).getInstitutions()
                    .stream().map(Institution::getId).toList());

            filter.setExternalId("foreign-absent");
            assertTrue(connector.findUserInstitutionAggregation(
                    filter, UserInstitutionAggregation.class).isEmpty());
        }
    }

    @Test
    void notificationsRespectOwnershipForUpsertRemovalAndCleanup() throws Exception {
        TenantContext tenantContext = new TenantContext();
        tenantContext.setTenantId("AR");
        TenantRegistry registry = new TenantRegistry(
                new ObjectMapper(),
                new MockEnvironment().withProperty(
                        "MONGO_AR",
                        "mongodb://" + MONGO.getHost() + ":" + MONGO.getMappedPort(27017)),
                """
                {"AR":{"mongo":{"account":"local","database":"tenant_mail_test","connectionStringEnvVar":"MONGO_AR"}}}
                """,
                "AR",
                "");
        registry.initialize();
        MongoTemplate mongoTemplate = new MongoTemplate(
                new TenantMongoDatabaseFactory(registry, tenantContext),
                mappingConverter());
        mongoTemplate.dropCollection(MailNotificationEntity.class);
        mongoTemplate.indexOps(MailNotificationEntity.class)
                .ensureIndex(new Index().on("institutionId", Sort.Direction.ASC).unique());
        mongoTemplate.insert(mailNotification("owned", "AR", "owned-product"));
        mongoTemplate.insert(mailNotification("legacy-absent", null, "legacy-product"));
        mongoTemplate.insert(mailNotification("legacy-null", null, "null-product"));
        mongoTemplate.insert(mailNotification("foreign", "PNPG", "foreign-product"));

        MailNotificationConnectorImpl strictConnector =
                mailNotificationConnector(mongoTemplate, tenantContext, true);
        assertTrue(strictConnector.addMailNotification(
                "new", "new-product", "new@example.test", 5));
        assertEquals("AR", mailDocument(mongoTemplate, "new").getString("tenantId"));
        assertTrue(strictConnector.addMailNotification(
                "owned", "additional-product", "owned@example.test", 5));
        assertEquals("AR", mailDocument(mongoTemplate, "owned").getString("tenantId"));
        assertFalse(strictConnector.removeMailNotification("legacy-absent", "legacy-product"));
        assertFalse(strictConnector.removeMailNotification("legacy-null", "null-product"));
        assertFalse(strictConnector.removeMailNotification("foreign", "foreign-product"));
        assertFalse(strictConnector.removeMailNotification("missing", "missing-product"));
        assertThrows(DuplicateKeyException.class, () -> strictConnector.addMailNotification(
                "foreign", "new-product", "foreign@example.test", 5));
        assertEquals("PNPG", mailDocument(mongoTemplate, "foreign").getString("tenantId"));
        assertEquals(List.of("foreign-product"),
                mailDocument(mongoTemplate, "foreign").getList("productIds", String.class));

        MailNotificationConnectorImpl compatibilityConnector =
                mailNotificationConnector(mongoTemplate, tenantContext, false);
        assertTrue(compatibilityConnector.addMailNotification(
                "new-compat", "new-product", "new-compat@example.test", 5));
        assertEquals("AR", mailDocument(mongoTemplate, "new-compat").getString("tenantId"));
        assertTrue(compatibilityConnector.addMailNotification(
                "legacy-absent", "additional-product", "legacy@example.test", 5));
        assertFalse(mailDocument(mongoTemplate, "legacy-absent").containsKey("tenantId"));
        assertTrue(compatibilityConnector.removeMailNotification(
                "legacy-null", "null-product"));
        assertFalse(mongoTemplate.exists(
                Query.query(org.springframework.data.mongodb.core.query.Criteria
                        .where("institutionId").is("legacy-null")),
                MailNotificationEntity.class));
        assertTrue(compatibilityConnector.removeMailNotification(
                "legacy-absent", "legacy-product"));
        assertTrue(compatibilityConnector.removeMailNotification(
                "legacy-absent", "additional-product"));
        assertFalse(mongoTemplate.exists(
                Query.query(org.springframework.data.mongodb.core.query.Criteria
                        .where("institutionId").is("legacy-absent")),
                MailNotificationEntity.class));
        assertFalse(compatibilityConnector.removeMailNotification("foreign", "foreign-product"));
        assertEquals("PNPG", mailDocument(mongoTemplate, "foreign").getString("tenantId"));
    }

    private MailNotificationConnectorImpl mailNotificationConnector(
            MongoTemplate mongoTemplate, TenantContext tenantContext, boolean strict) {
        TenantOwnershipPolicy policy = new TenantOwnershipPolicy(tenantContext, strict);
        MongoCustomConnectorImpl customConnector =
                new MongoCustomConnectorImpl(mongoTemplate, policy);
        MailNotificationRepository repository = mock(MailNotificationRepository.class);
        when(repository.upsert(any(Query.class), any(UpdateDefinition.class),
                eq(MailNotificationEntity.class))).thenAnswer(invocation ->
                customConnector.upsert(
                        invocation.getArgument(0),
                        invocation.getArgument(1),
                        MailNotificationEntity.class));
        when(repository.findAndModify(
                any(Query.class),
                any(UpdateDefinition.class),
                any(FindAndModifyOptions.class),
                eq(MailNotificationEntity.class))).thenAnswer(invocation ->
                customConnector.findAndModify(
                        invocation.getArgument(0),
                        invocation.getArgument(1),
                        invocation.getArgument(2),
                        MailNotificationEntity.class));
        when(repository.findAndRemove(
                any(Query.class), eq(MailNotificationEntity.class))).thenAnswer(invocation ->
                customConnector.findAndRemove(
                        invocation.getArgument(0), MailNotificationEntity.class));
        return new MailNotificationConnectorImpl(repository, tenantContext);
    }

    private MailNotificationEntity mailNotification(
            String institutionId, String tenantId, String productId) {
        MailNotificationEntity entity = new MailNotificationEntity();
        entity.setInstitutionId(institutionId);
        entity.setTenantId(tenantId);
        entity.setProductIds(Set.of(productId));
        entity.setModuleDayOfTheEpoch(5);
        entity.setDigitalAddress(institutionId + "@example.test");
        entity.setCreatedAt(OffsetDateTime.parse("2025-01-01T00:00:00Z").toInstant());
        entity.setUpdatedAt(OffsetDateTime.parse("2025-01-01T00:00:00Z").toInstant());
        return entity;
    }

    private org.bson.Document mailDocument(MongoTemplate mongoTemplate, String institutionId) {
        return Objects.requireNonNull(mongoTemplate.getCollection("MailNotification")
                .find(new org.bson.Document("institutionId", institutionId))
                .first());
    }

    private InstitutionEntity institution(String id, String tenantId) {
        InstitutionEntity entity = new InstitutionEntity();
        entity.setId(id);
        entity.setTenantId(tenantId);
        entity.setDescription(id);
        return entity;
    }

    private DelegationEntity delegationWithReferences(
            String id, String from, String to, String createdAt) {
        DelegationEntity entity = delegation(id, "AR", "prod-a", createdAt);
        entity.setFrom(from);
        entity.setTo(to);
        entity.setType(DelegationType.EA);
        return entity;
    }

    private MappingMongoConverter mappingConverter() throws Exception {
        MongoCustomConversions conversions = new MongoCustomConversions(List.of(
                new OffsetDateTimeToStringConverter(),
                new StringToOffsetDateTimeConverter()));
        MongoMappingContext mappingContext = new MongoMappingContext();
        mappingContext.setInitialEntitySet(Set.of(
                InstitutionEntity.class,
                MailNotificationEntity.class,
                DelegationEntity.class,
                DelegationPageEntity.class,
                DelegationCount.class));
        mappingContext.setSimpleTypeHolder(conversions.getSimpleTypeHolder());
        mappingContext.afterPropertiesSet();
        MappingMongoConverter converter =
                new MappingMongoConverter(NoOpDbRefResolver.INSTANCE, mappingContext);
        converter.setCustomConversions(conversions);
        converter.afterPropertiesSet();
        return converter;
    }

    private DelegationEntity delegation(
            String id, String tenantId, String productId, String createdAt) {
        DelegationEntity entity = new DelegationEntity();
        entity.setId(id);
        entity.setTenantId(tenantId);
        entity.setProductId(productId);
        entity.setCreatedAt(OffsetDateTime.parse(createdAt));
        entity.setStatus(ACTIVE);
        return entity;
    }

    private long countFor(List<DelegationCount> counts, String productId) {
        return counts.stream()
                .filter(count -> productId.equals(count.getProductId()))
                .mapToLong(DelegationCount::getCount)
                .findFirst()
                .orElse(0L);
    }

    private static class OffsetDateTimeToStringConverter
            implements Converter<OffsetDateTime, String> {
        @Override
        public String convert(OffsetDateTime source) {
            return source.toInstant().atOffset(ZoneOffset.UTC)
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        }
    }

    private static class StringToOffsetDateTimeConverter
            implements Converter<String, OffsetDateTime> {
        @Override
        public OffsetDateTime convert(String source) {
            return OffsetDateTime.parse(source, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        }
    }
}
