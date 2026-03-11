package it.pagopa.selfcare.user_group;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@SpringBootApplication
@EnableMongoAuditing(modifyOnCreate = false)
public class SelfCareUserGroupApplication {

    public static void main(String[] args) {
        SpringApplication.run(SelfCareUserGroupApplication.class, args);
    }

}
