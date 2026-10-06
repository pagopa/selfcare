package it.pagopa.selfcare.user_group;

import it.pagopa.selfcare.commons.tenant.TenantConfiguration;
import it.pagopa.selfcare.commons.tenant.mongodb.TenantMongoConfiguration;
import it.pagopa.selfcare.commons.tenant.storage.TenantStorageConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@SpringBootApplication(exclude = MongoAutoConfiguration.class)
@EnableMongoAuditing(modifyOnCreate = false)
@ComponentScan(basePackages = {"it.pagopa.selfcare.cucumber.utils", "it.pagopa.selfcare.user_group"})
@Import({
        TenantConfiguration.class,
        TenantMongoConfiguration.class,
        TenantStorageConfiguration.class
})
public class SelfCareUserGroupApplication {

    public static void main(String[] args) {
        SpringApplication.run(SelfCareUserGroupApplication.class, args);
    }

}
