package it.pagopa.selfcare.mscore.connector.dao;

import it.pagopa.selfcare.mscore.connector.dao.model.InstitutionEntity;
import it.pagopa.selfcare.mscore.connector.dao.model.mapper.InstitutionEntityMapperImpl;
import it.pagopa.selfcare.mscore.exception.InvalidRequestException;
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

import static org.junit.jupiter.api.Assertions.*;

@DataMongoTest
@Import({
        CurrentTenantProvider.class,
        TenantDataIsolation.class,
        InstitutionConnectorImpl.class,
        InstitutionEntityMapperImpl.class
})
class TenantDataIsolationRepositoryTest {

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
    void recordWrittenUnderTenantAIsStampedAndNotVisibleToTenantB() {
        bindTenant(TestTenant.AR);

        Institution saved = institutionConnector.save(institution("tenant-a", "external-a", "Tenant A"));

        InstitutionEntity rawSaved = mongoTemplate.findById(saved.getId(), InstitutionEntity.class);
        assertNotNull(rawSaved);
        assertEquals(TestTenant.AR.name(), rawSaved.getTenantId());

        bindTenant(TestTenant.PNPG);
        assertThrows(ResourceNotFoundException.class, () -> institutionConnector.findById(saved.getId()));
    }

    @Test
    void legacyRecordWithoutTenantIdIsVisibleDuringMigration() {
        InstitutionEntity legacy = institutionEntity("legacy", "legacy-ext", "Legacy", null);
        mongoTemplate.save(legacy);

        bindTenant(TestTenant.PNPG);

        Institution result = institutionConnector.findById("legacy");
        assertEquals("legacy", result.getId());
    }

    @Test
    void saveRejectsWriteOnEntityOwnedByAnotherTenant() {
        mongoTemplate.save(institutionEntity("existing", "existing-ext", "Original", TestTenant.PNPG.name()));
        bindTenant(TestTenant.AR);

        Institution attemptedUpdate = institution("existing", "changed-ext", "Changed");
        assertThrows(InvalidRequestException.class, () -> institutionConnector.save(attemptedUpdate));

        InstitutionEntity unchanged = mongoTemplate.findById("existing", InstitutionEntity.class);
        assertNotNull(unchanged);
        assertEquals(TestTenant.PNPG.name(), unchanged.getTenantId());
        assertEquals("Original", unchanged.getDescription());
    }

    @Test
    void noTenantContextLeavesReadsUnscopedDuringMigration() {
        mongoTemplate.save(institutionEntity("ar", "ar-ext", "AR", TestTenant.AR.name()));
        mongoTemplate.save(institutionEntity("pnpg", "pnpg-ext", "PNPG", TestTenant.PNPG.name()));
        RequestContextHolder.resetRequestAttributes();

        List<Institution> institutions = institutionConnector.findAll();

        assertEquals(2, institutions.size());
    }

    private void bindTenant(TestTenant tenant) {
        RequestContextHolder.setRequestAttributes(new TenantRequestAttributes(tenant));
    }

    private Institution institution(String id, String externalId, String description) {
        Institution institution = new Institution();
        institution.setId(id);
        institution.setExternalId(externalId);
        institution.setDescription(description);
        return institution;
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
