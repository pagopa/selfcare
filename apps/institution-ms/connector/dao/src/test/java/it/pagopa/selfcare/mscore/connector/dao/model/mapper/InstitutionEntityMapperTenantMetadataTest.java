package it.pagopa.selfcare.mscore.connector.dao.model.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.mscore.connector.dao.model.DelegationEntity;
import it.pagopa.selfcare.mscore.connector.dao.model.InstitutionEntity;
import it.pagopa.selfcare.mscore.connector.dao.model.MailNotificationEntity;
import it.pagopa.selfcare.mscore.model.institution.Institution;
import org.junit.jupiter.api.Test;
import org.bson.Document;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class InstitutionEntityMapperTenantMetadataTest {

    @Test
    void tenantOwnershipMetadataIsPersistedForAllEntities() throws Exception {
        MappingMongoConverter converter = mappingConverter();
        InstitutionEntity entity = new InstitutionEntity();
        entity.setTenantId("AR");
        DelegationEntity delegation = new DelegationEntity();
        delegation.setTenantId("AR");
        MailNotificationEntity notification = new MailNotificationEntity();
        notification.setTenantId("AR");

        assertEquals("AR", toDocument(converter, entity).getString("tenantId"));
        assertEquals("AR", toDocument(converter, delegation).getString("tenantId"));
        assertEquals("AR", toDocument(converter, notification).getString("tenantId"));
    }

    @Test
    void tenantOwnershipMetadataIsNotExposedByInstitutionModel() throws Exception {
        InstitutionEntity entity = new InstitutionEntity();
        entity.setTenantId("AR");
        Institution institution = new InstitutionEntityMapperImpl()
                .convertToInstitution(entity);

        assertFalse(new ObjectMapper().writeValueAsString(institution).contains("tenantId"));
    }

    private MappingMongoConverter mappingConverter() throws Exception {
        MongoMappingContext mappingContext = new MongoMappingContext();
        mappingContext.setInitialEntitySet(Set.of(
                InstitutionEntity.class,
                DelegationEntity.class,
                MailNotificationEntity.class));
        MongoCustomConversions conversions = MongoCustomConversions.create(configuration -> {});
        mappingContext.setSimpleTypeHolder(conversions.getSimpleTypeHolder());
        mappingContext.afterPropertiesSet();
        MappingMongoConverter converter = new MappingMongoConverter(
                NoOpDbRefResolver.INSTANCE,
                mappingContext);
        converter.setCustomConversions(conversions);
        converter.afterPropertiesSet();
        return converter;
    }

    private Document toDocument(MappingMongoConverter converter, Object entity) {
        Document document = new Document();
        converter.write(entity, document);
        return document;
    }
}
