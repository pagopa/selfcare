package it.pagopa.selfcare.commons.tenant.mongodb;

import it.pagopa.selfcare.commons.tenant.TenantContext;
import it.pagopa.selfcare.commons.tenant.TenantRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration
@ConditionalOnProperty(name = "tenant.mongodb.enabled", havingValue = "true")
public class TenantMongoConfiguration {

    @Bean
    @Primary
    MongoDatabaseFactory mongoDatabaseFactory(
            TenantRegistry tenantRegistry, TenantContext tenantContext) {
        return new TenantMongoDatabaseFactory(tenantRegistry, tenantContext);
    }

    @Bean
    @Primary
    MongoTemplate mongoTemplate(MongoDatabaseFactory mongoDatabaseFactory) {
        return new MongoTemplate(mongoDatabaseFactory);
    }
}
