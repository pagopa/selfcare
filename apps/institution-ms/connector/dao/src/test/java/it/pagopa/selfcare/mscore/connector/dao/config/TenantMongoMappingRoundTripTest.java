package it.pagopa.selfcare.mscore.connector.dao.config;

import it.pagopa.selfcare.commons.tenant.TenantContext;
import it.pagopa.selfcare.commons.tenant.TenantRegistry;
import it.pagopa.selfcare.commons.tenant.mongodb.TenantMongoConfiguration;
import it.pagopa.selfcare.commons.tenant.mongodb.TenantMongoDatabaseFactory;
import it.pagopa.selfcare.mscore.connector.dao.model.DelegationEntity;
import it.pagopa.selfcare.mscore.connector.dao.model.InstitutionEntity;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.mock.env.MockEnvironment;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TenantMongoMappingRoundTripTest {

    @Test
    void routedTemplateRetainsInstitutionAndDelegationOffsetDateTimeMapping() throws Exception {
        TenantContext tenantContext = new TenantContext();
        TenantRegistry registry = new TenantRegistry(
                new com.fasterxml.jackson.databind.ObjectMapper(),
                new MockEnvironment().withProperty("MONGO_AR", "mongodb://localhost:27017"),
                """
                {"AR":{"mongo":{"account":"local","database":"institution","connectionStringEnvVar":"MONGO_AR"}}}
                """,
                "AR",
                "");
        registry.initialize();
        TenantMongoDatabaseFactory databaseFactory =
                new TenantMongoDatabaseFactory(registry, tenantContext);
        MongoTemplate template =
                new MongoTemplate(databaseFactory, mappingConverter(new DaoConfig().customConversions()));
        MappingMongoConverter converter = (MappingMongoConverter) template.getConverter();

        OffsetDateTime institutionTime =
                OffsetDateTime.parse("2025-02-03T04:05:06+05:30");
        InstitutionEntity institution = new InstitutionEntity();
        institution.setId("institution-1");
        institution.setCreatedAt(institutionTime);
        Document institutionDocument = new Document();
        converter.write(institution, institutionDocument);

        assertEquals("2025-02-02T22:35:06Z",
                institutionDocument.getString("createdAt"));
        InstitutionEntity restoredInstitution =
                converter.read(InstitutionEntity.class, institutionDocument);
        assertEquals(institutionTime.toInstant(),
                restoredInstitution.getCreatedAt().toInstant());
        assertEquals(ZoneOffset.UTC, restoredInstitution.getCreatedAt().getOffset());

        OffsetDateTime delegationTime =
                OffsetDateTime.parse("2025-02-03T04:05:06-07:00");
        DelegationEntity delegation = new DelegationEntity();
        delegation.setId("delegation-1");
        delegation.setUpdatedAt(delegationTime);
        Document delegationDocument = new Document();
        converter.write(delegation, delegationDocument);

        assertEquals("2025-02-03T11:05:06Z",
                delegationDocument.getString("updatedAt"));
        DelegationEntity restoredDelegation =
                converter.read(DelegationEntity.class, delegationDocument);
        assertEquals(delegationTime.toInstant(),
                restoredDelegation.getUpdatedAt().toInstant());
        assertEquals(ZoneOffset.UTC, restoredDelegation.getUpdatedAt().getOffset());
    }

    private MappingMongoConverter mappingConverter(MongoCustomConversions conversions)
            throws Exception {
        MongoMappingContext mappingContext = new MongoMappingContext();
        mappingContext.setInitialEntitySet(Set.of(
                InstitutionEntity.class,
                DelegationEntity.class));
        mappingContext.setSimpleTypeHolder(conversions.getSimpleTypeHolder());
        mappingContext.afterPropertiesSet();
        MappingMongoConverter converter =
                new MappingMongoConverter(NoOpDbRefResolver.INSTANCE, mappingContext);
        converter.setCustomConversions(conversions);
        converter.afterPropertiesSet();
        return converter;
    }
}
