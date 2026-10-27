package it.pagopa.selfcare.mscore;

import it.pagopa.selfcare.commons.tenant.TenantConfiguration;
import it.pagopa.selfcare.commons.tenant.mongodb.TenantMongoConfiguration;
import it.pagopa.selfcare.commons.tenant.storage.TenantStorageConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(exclude = MongoAutoConfiguration.class)
@EnableAsync
@ComponentScan(basePackages = {"it.pagopa.selfcare.cucumber.utils", "it.pagopa.selfcare.mscore"})
@Import({
        TenantConfiguration.class,
        TenantMongoConfiguration.class,
        TenantStorageConfiguration.class
})
public class SelfCareCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(SelfCareCoreApplication.class, args);
    }

}
