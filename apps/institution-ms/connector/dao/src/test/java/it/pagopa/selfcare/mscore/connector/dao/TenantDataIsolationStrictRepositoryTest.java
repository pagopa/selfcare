package it.pagopa.selfcare.mscore.connector.dao;

import it.pagopa.selfcare.mscore.connector.dao.model.InstitutionEntity;
import it.pagopa.selfcare.mscore.connector.dao.model.mapper.InstitutionEntityMapperImpl;
import it.pagopa.selfcare.mscore.exception.ResourceNotFoundException;
import it.pagopa.selfcare.mscore.model.institution.Institution;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Pins the post-backfill behaviour, with {@code selfcare.tenant.strict-data-isolation} turned on.
 *
 * <p>{@link TenantDataIsolationRepositoryTest} covers the migration phase, where an untagged record
 * is still readable by whichever tenant asks. That leniency is what the backfill exists to remove
 * and is deliberately the default, so without this class the strict path would first be exercised
 * in whichever environment flipped the flag. Here the same reads are checked against the behaviour
 * that makes the isolation a boundary: an untagged record belongs to nobody.
 */
@DataMongoTest(properties = "selfcare.tenant.strict-data-isolation=true")
@Import({
        CurrentTenantProvider.class,
        TenantDataIsolation.class,
        InstitutionConnectorImpl.class,
        InstitutionEntityMapperImpl.class
})
class TenantDataIsolationStrictRepositoryTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {
    }

    private enum TestTenant {
        AR,
        PNPG
    }

    private static final MongoDBContainer MONGO = new MongoDBContainer(
            DockerImageName.parse("mongo:latest@sha256:1cb283500219e8fc0b61b328ea5a199a395a753d88b17351c58874fb425223cb")
                    .asCompatibleSubstituteFor("mongo"));

    static {
        MONGO.start();
    }

    @Autowired
    private InstitutionConnectorImpl institutionConnector;

    @Autowired
    private MongoTemplate mongoTemplate;

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", MONGO::getReplicaSetUrl);
    }

    @BeforeEach
    void setUp() {
        mongoTemplate.dropCollection(InstitutionEntity.class);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @AfterAll
    static void stopMongo() {
        MONGO.stop();
    }

    @Test
    void legacyRecordWithoutTenantIdIsNoLongerVisible() {
        mongoTemplate.save(institutionEntity("legacy", "legacy-ext", "Legacy", null));

        bindTenant(TestTenant.PNPG);

        assertThrows(ResourceNotFoundException.class, () -> institutionConnector.findById("legacy"));
    }

    @Test
    void recordOfCurrentTenantIsStillVisible() {
        mongoTemplate.save(institutionEntity("ar", "ar-ext", "AR", TestTenant.AR.name()));

        bindTenant(TestTenant.AR);

        assertEquals("ar", institutionConnector.findById("ar").getId());
    }

    @Test
    void recordOfAnotherTenantStaysInvisible() {
        mongoTemplate.save(institutionEntity("pnpg", "pnpg-ext", "PNPG", TestTenant.PNPG.name()));

        bindTenant(TestTenant.AR);

        assertThrows(ResourceNotFoundException.class, () -> institutionConnector.findById("pnpg"));
    }

    /**
     * Strictness applies to the tenant predicate, not to callers that have no tenant at all:
     * schedulers and consumers still read unscoped. Removing that concession is a separate decision
     * from the backfill, so the flag deliberately leaves it alone.
     */
    @Test
    void noTenantContextStillLeavesReadsUnscoped() {
        mongoTemplate.save(institutionEntity("ar", "ar-ext", "AR", TestTenant.AR.name()));
        mongoTemplate.save(institutionEntity("legacy", "legacy-ext", "Legacy", null));
        RequestContextHolder.resetRequestAttributes();

        List<Institution> institutions = institutionConnector.findAll();

        assertEquals(2, institutions.size());
    }

    private void bindTenant(TestTenant tenant) {
        RequestContextHolder.setRequestAttributes(new TenantRequestAttributes(tenant));
    }

    private InstitutionEntity institutionEntity(String id, String externalId, String description, String tenantId) {
        InstitutionEntity entity = new InstitutionEntity();
        entity.setId(id);
        entity.setExternalId(externalId);
        entity.setDescription(description);
        entity.setTenantId(tenantId);
        return entity;
    }

    private record TenantRequestAttributes(TestTenant tenant) implements RequestAttributes {

        @Override
        public Object getAttribute(String name, int scope) {
            return SCOPE_REQUEST == scope && CurrentTenantProvider.TENANT_REQUEST_ATTRIBUTE.equals(name) ? tenant : null;
        }

        @Override
        public void setAttribute(String name, Object value, int scope) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void removeAttribute(String name, int scope) {
        }

        @Override
        public String[] getAttributeNames(int scope) {
            return new String[]{CurrentTenantProvider.TENANT_REQUEST_ATTRIBUTE};
        }

        @Override
        public void registerDestructionCallback(String name, Runnable callback, int scope) {
        }

        @Override
        public Object resolveReference(String key) {
            return null;
        }

        @Override
        public String getSessionId() {
            return "test";
        }

        @Override
        public Object getSessionMutex() {
            return this;
        }
    }
}
