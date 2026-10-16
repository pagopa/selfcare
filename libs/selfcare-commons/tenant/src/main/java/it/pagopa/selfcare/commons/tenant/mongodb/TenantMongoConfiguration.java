package it.pagopa.selfcare.commons.tenant.mongodb;

import it.pagopa.selfcare.commons.tenant.TenantContext;
import it.pagopa.selfcare.commons.tenant.TenantRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration
@ConditionalOnProperty(name = "tenant.mongodb.enabled", havingValue = "true")
public class TenantMongoConfiguration {

    @Bean
    @Primary
    MongoDatabaseFactory mongoDatabaseFactory(
            TenantRegistry tenantRegistry,
            TenantContext tenantContext,
            @Value("${tenant.mongodb.application-name:}") String applicationName) {
        return new TenantMongoDatabaseFactory(tenantRegistry, tenantContext, applicationName);
    }

    @Bean
    @Primary
    MongoTemplate mongoTemplate(
            MongoDatabaseFactory mongoDatabaseFactory, MongoConverter mongoConverter) {
        return new MongoTemplate(mongoDatabaseFactory, mongoConverter);
    }
}
