package it.pagopa.selfcare.applicationcontext;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.commons.tenant.TenantContext;
import it.pagopa.selfcare.commons.tenant.TenantRegistry;
import it.pagopa.selfcare.commons.tenant.mongodb.TenantMongoConfiguration;
import it.pagopa.selfcare.mscore.connector.dao.TenantOwnershipPolicy;
import it.pagopa.selfcare.mscore.connector.dao.InstitutionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.mock.env.MockEnvironment;

class TenantMongoApplicationContextTest {

    @Test
    void tenantConfigurationProvidesOneRoutedMongoTemplate() {
        new ApplicationContextRunner()
                .withUserConfiguration(TenantMongoConfiguration.class, TestBeans.class)
                .withPropertyValues("tenant.mongodb.enabled=true")
                .run(context -> {
                    assertInstanceOf(MongoDatabaseFactory.class, context.getBean(MongoDatabaseFactory.class));
                    MongoTemplate template = context.getBean(MongoTemplate.class);
                    assertSame(template, context.getBean(MongoOperations.class));
                    assertSame(context.getBean(MongoDatabaseFactory.class), template.getMongoDatabaseFactory());
                    assertSame(template, context.getBean("mongoTemplate"));
                    assertInstanceOf(InstitutionRepository.class, context.getBean(InstitutionRepository.class));
                    assertSame(1, context.getBeansOfType(MongoDatabaseFactory.class).size());
                    assertSame(1, context.getBeansOfType(MongoOperations.class).size());
                });
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableMongoRepositories(basePackageClasses = InstitutionRepository.class)
    static class TestBeans {

        @Bean
        TenantContext tenantContext() {
            return new TenantContext();
        }

        @Bean
        TenantOwnershipPolicy tenantOwnershipPolicy(TenantContext tenantContext) {
            return new TenantOwnershipPolicy(tenantContext, true);
        }

        @Bean
        TenantRegistry tenantRegistry() {
            MockEnvironment environment =
                    new MockEnvironment().withProperty("MONGO_AR", "mongodb://localhost:27017");
            TenantRegistry registry = new TenantRegistry(
                    new ObjectMapper(),
                    environment,
                    """
                    {"AR":{"mongo":{"account":"local","database":"institution","connectionStringEnvVar":"MONGO_AR"}}}
                    """,
                    "AR",
                    "");
            registry.initialize();
            return registry;
        }

        @Bean
        MongoConverter mongoConverter() throws Exception {
            MongoCustomConversions conversions = MongoCustomConversions.create(adapter -> {});
            MongoMappingContext mappingContext = new MongoMappingContext();
            mappingContext.setSimpleTypeHolder(conversions.getSimpleTypeHolder());
            mappingContext.afterPropertiesSet();
            MappingMongoConverter converter =
                    new MappingMongoConverter(NoOpDbRefResolver.INSTANCE, mappingContext);
            converter.setCustomConversions(conversions);
            converter.afterPropertiesSet();
            return converter;
        }
    }
}
