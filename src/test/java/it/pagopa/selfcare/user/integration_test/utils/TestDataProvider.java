package it.pagopa.selfcare.user.integration_test.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.user.integration_test.model.TestData;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;

@ApplicationScoped
@Slf4j
public class TestDataProvider {

    private final TestData testData;

    public TestDataProvider() throws IOException {
        testData = readTestData();
    }

    public TestData getTestData() {
        return testData;
    }

    private TestData readTestData() throws IOException {
        log.info("Reading test data");
        return new ObjectMapper().readValue(new File("src/test/resources/testData.json"), TestData.class);
    }

}
