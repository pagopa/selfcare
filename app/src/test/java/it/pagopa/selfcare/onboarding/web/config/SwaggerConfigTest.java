package it.pagopa.selfcare.onboarding.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.onboarding.connector.api.ProductsConnector;
import it.pagopa.selfcare.onboarding.connector.rest.client.*;
import it.pagopa.selfcare.onboarding.connector.rest.config.ProductServiceConfig;
import it.pagopa.selfcare.onboarding.connector.rest.config.UserRegistryRestClientConfig;
import it.pagopa.selfcare.onboarding.core.InstitutionService;
import it.pagopa.selfcare.onboarding.core.ProductAzureService;
import it.pagopa.selfcare.onboarding.core.TokenService;
import it.pagopa.selfcare.onboarding.core.UserService;
import it.pagopa.selfcare.product.service.ProductService;
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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ComponentScan(basePackages = {
        "it.pagopa.selfcare.onboarding.web.controller",
        "it.pagopa.selfcare.onboarding.web.model.mapper",
})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:config/application.yml")
class SwaggerConfigTest {

    @MockBean
    private InstitutionService institutionService;

    @MockBean
    private ProductAzureService productAzureService;

    @MockBean
    private ProductsConnector productsConnector;

    @MockBean
    private UserService userService;

    @MockBean
    private TokenService tokenService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserRegistryRestClient userRegistryRestClient;

    @MockBean
    private PartyProcessRestClient partyProcessRestClient;

    @MockBean
    private OnboardingFunctionsApiClient onboardingFunctionsApiClient;

    @MockBean
    private MsUserApiClient msUserApiClient;

    @MockBean
    private MsOnboardingTokenApiClient msOnboardingTokenApiClient;

    @MockBean
    private MsOnboardingAggregatesApiClient msOnboardingAggregatesApiClient;

    @MockBean
    private MsOnboardingSupportApiClient msOnboardingSupportApiClient;

    @MockBean
    private MsOnboardingApiClient msOnboardingApiClient;

    @MockBean
    private MsOnboardingBillingApiClient msOnboardingBillingApiClient;

    @MockBean
    private MsCoreRestClient msCoreRestClient;

    @MockBean
    private MsProductApiClient msProductApiClient;

    @MockBean
    private ProductService productService;

    @MockBean
    private PartyRegistryProxyRestClient partyRegistryProxyRestClient;

    @MockBean
    private UserRegistryRestClientConfig userRegistryRestClientConfig;

    @MockBean
    private ProductServiceConfig productServiceConfig;

    @Autowired
    WebApplicationContext context;

    @Test
    void testApiDocsEndpoint() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        mockMvc.perform(MockMvcRequestBuilders.get("/v3/api-docs").accept(MediaType.APPLICATION_JSON))
                .andDo(result -> {
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

