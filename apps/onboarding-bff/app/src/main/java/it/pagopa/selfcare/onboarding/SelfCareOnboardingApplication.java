package it.pagopa.selfcare.onboarding;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "it.pagopa.selfcare")
public class SelfCareOnboardingApplication {

    public static void main(String[] args) {
        SpringApplication.run(SelfCareOnboardingApplication.class, args);
    }

}
