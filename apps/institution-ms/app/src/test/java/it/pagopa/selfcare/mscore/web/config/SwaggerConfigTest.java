package it.pagopa.selfcare.mscore.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.mscore.connector.azure_storage.AzureBlobClient;
import it.pagopa.selfcare.mscore.connector.rest.UserRegistryConnectorImpl;
import it.pagopa.selfcare.mscore.core.*;
import it.pagopa.selfcare.mscore.web.util.EncryptedTaxCodeParamResolver;
import it.pagopa.selfcare.product.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Slf4j
class SwaggerConfigTest {

    @MockBean
    ExternalService externalService;

    @MockBean
    InstitutionService institutionService;

    @MockBean
    OnboardingService onboardingService;

    @MockBean
    AzureBlobClient azureBlobClient;

    @MockBean
    ProductService productService;

    @Autowired
    WebApplicationContext context;

    @MockBean
    DelegationService delegationService;

    @MockBean
    EventsService eventsService;

    @MockBean
    private EncryptedTaxCodeParamResolver encryptedTaxCodeParamResolver;

    @MockBean
    private UserRegistryConnectorImpl userRegistryConnector;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void swaggerSpringPlugin() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        mockMvc.perform(MockMvcRequestBuilders.get("/v3/api-docs").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful()).andDo(result -> {
                    assertNotNull(result);
                    assertNotNull(result.getResponse());
                    final String content = result.getResponse().getContentAsString();
                    checkPlaceholders(content);
                    assertFalse(content.isBlank());
                    assertFalse(content.contains("${"), "Generated swagger contains placeholders");
                    Object swagger = objectMapper.readValue(result.getResponse().getContentAsString(), Object.class);
                    String formatted = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(swagger);
                    Path basePath = Paths.get("src/main/docs/");
                    Files.createDirectories(basePath);
                    Files.write(basePath.resolve("openapi.json"), formatted.getBytes());
                });
    }

    private static void checkPlaceholders(String content) {
        Pattern pattern = Pattern.compile("\\$\\{(.*?)\\}");
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            log.error("\033[31m An error occurred with placeholder: {}", matcher.group(1));
        }
    }
}

