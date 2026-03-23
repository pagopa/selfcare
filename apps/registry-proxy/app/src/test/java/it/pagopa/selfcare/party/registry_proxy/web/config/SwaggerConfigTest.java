package it.pagopa.selfcare.party.registry_proxy.web.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.party.registry_proxy.connector.api.GeoTaxonomiesConnector;
import it.pagopa.selfcare.party.registry_proxy.connector.api.NationalRegistriesConnector;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.IvassConnectorImpl;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.config.GeoTaxonomiesRestClientConfig;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.config.NationalRegistriesRestClientConfig;
import it.pagopa.selfcare.party.registry_proxy.core.*;
import it.pagopa.selfcare.party.registry_proxy.web.controller.EventController;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT)
@ComponentScan(basePackages = {"it.pagopa.selfcare.party.registry_proxy.web.controller", "it.pagopa.selfcare.party.registry_proxy.web.model.mapper"})
@TestPropertySource(locations = "classpath:config/application.yml", properties = {"ivass.file.connector.type=rest"})
class SwaggerConfigTest {

    @MockBean
    private InstitutionService institutionService;

    @MockBean
    private StationService StationService;

    @MockBean
    private IvassService ivassService;

    @MockBean
    private CategoryService categoryService;

    @MockBean
    private NationalRegistriesService nationalRegistriesService;

    @MockBean
    private GeoTaxonomiesConnector geoTaxonomiesConnectorImpl;

    @MockBean
    private NationalRegistriesConnector nationalRegistriesConnectorImpl;

    @MockBean
    private GeographicTaxonomiesService geographicTaxonomiesService;

    @MockBean
    private GeoTaxonomiesRestClientConfig geoTaxonomiesRestClientConfig;

    @MockBean
    private NationalRegistriesRestClientConfig nationalRegistriesRestClientConfig;

    @MockBean
    private AOOService aooService;

    @MockBean
    private UOService uoService;

    @MockBean
    private ANACService anacService;

    @MockBean
    private IvassConnectorImpl ivassConnector;

    @MockBean
    private PDNDInfoCamereService pdndInfoCamereService;

    @MockBean
    private EventController eventController;

    @MockBean
    private ApplicationInsightsLogger applicationInsightsLogger;

    @Autowired
    WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void swaggerSpringPlugin() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        mockMvc.perform(MockMvcRequestBuilders.get("/v3/api-docs").accept(MediaType.APPLICATION_JSON))
                .andDo((result) -> {
                    assertNotNull(result);
                    assertNotNull(result.getResponse());
                    final String content = result.getResponse().getContentAsString();
                    assertFalse(content.isBlank());
                    assertFalse(content.contains("${"), "Generated swagger contains placeholders");
                    Object swagger = objectMapper.readValue(content, Object.class);
                    String formatted = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(swagger);
                    Path basePath = Paths.get("src/main/resources/swagger/");
                    Files.createDirectories(basePath);
                    Files.write(basePath.resolve("api-docs.json"), formatted.getBytes());
                });
    }
}
