package it.pagopa.selfcare.user_group;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@SpringBootApplication
@EnableMongoAuditing(modifyOnCreate = false)
@ComponentScan(basePackages = {"it.pagopa.selfcare.cucumber.utils", "it.pagopa.selfcare.user_group"})
public class SelfCareUserGroupApplication {

    public static void main(String[] args) {
        SpringApplication.run(SelfCareUserGroupApplication.class, args);
    }

}
