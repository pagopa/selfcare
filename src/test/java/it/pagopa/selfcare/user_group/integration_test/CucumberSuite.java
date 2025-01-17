package it.pagopa.selfcare.user_group.integration_test;

import io.cucumber.spring.CucumberContextConfiguration;
import it.pagopa.selfcare.user_group.SelfCareUserGroupApplication;
import org.junit.platform.suite.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty")
@CucumberContextConfiguration
@SpringBootTest(classes = {SelfCareUserGroupApplication.class})
@TestPropertySource(locations = "classpath:application-test.properties")
@ExcludeTags({"FeatureCreate","FeatureRetrieve", "FeatureUpdate", "FeatureMembers"})
public class CucumberSuite {
}

