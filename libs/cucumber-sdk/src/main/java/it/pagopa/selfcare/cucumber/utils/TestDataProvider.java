package it.pagopa.selfcare.cucumber.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.cucumber.utils.model.TestData;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Getter
@Component
@ApplicationScoped
public class TestDataProvider {

    private final TestData testData;

    public TestDataProvider() throws IOException {
        testData = readTestData();
    }

    private TestData readTestData() throws IOException {
        log.info("Reading test data");
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream("testData.json")) {
            if (inputStream == null) {
                throw new IOException("File not found in classpath");
            }
            return new ObjectMapper().readValue(inputStream, TestData.class);
        }
    }
}
