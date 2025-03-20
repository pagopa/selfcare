package it.pagopa.selfcare.cucumber.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.cucumber.utils.model.TestData;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;

@Slf4j
@Getter
public class TestDataProvider {

    private final TestData testData;

    public TestDataProvider() throws IOException {
        testData = readTestData();
    }

    private TestData readTestData() throws IOException {
        log.info("Reading test data");
        return new ObjectMapper().readValue(new File("src/test/resources/testData.json"), TestData.class);
    }

}
