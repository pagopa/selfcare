package it.pagopa.selfcare.mscore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@ComponentScan(basePackages = {"it.pagopa.selfcare.cucumber.utils", "it.pagopa.selfcare.mscore"})
public class SelfCareCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(SelfCareCoreApplication.class, args);
    }

}
