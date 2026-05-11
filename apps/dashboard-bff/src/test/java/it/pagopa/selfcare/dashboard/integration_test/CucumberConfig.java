package it.pagopa.selfcare.dashboard.integration_test;

import io.restassured.RestAssured;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CucumberConfig {

    public CucumberConfig( @Value("${rest-assured.base-url}") String restAssuredBaseUrl, @Value("${rest-assured.port}")int restAssuredPort) {
        RestAssured.baseURI = restAssuredBaseUrl;
        RestAssured.port = restAssuredPort;
    }

}
