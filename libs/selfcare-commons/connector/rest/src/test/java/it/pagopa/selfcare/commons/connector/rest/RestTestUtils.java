package it.pagopa.selfcare.commons.connector.rest;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import com.github.tomakehurst.wiremock.standalone.JsonFileMappingsSource;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

public class RestTestUtils {

    public static WireMockConfiguration getWireMockConfiguration(String stubsPath) {
        String port = System.getenv("WIREMOCKPORT");
        WireMockConfiguration config = wireMockConfig()
                .port(port != null ? Integer.parseInt(port) : 0)
                .bindAddress("localhost")
                .withRootDirectory("src/test/resources")
                .extensions(new ResponseTemplateTransformer(false));
        config.mappingSource(new JsonFileMappingsSource(config.filesRoot().child(stubsPath)));
        return config;
    }

}
