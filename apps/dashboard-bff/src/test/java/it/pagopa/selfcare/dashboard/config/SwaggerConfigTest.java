package it.pagopa.selfcare.dashboard.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.dashboard.model.mapper.*;
import it.pagopa.selfcare.dashboard.security.ExchangeTokenServiceV2;
import it.pagopa.selfcare.dashboard.service.*;
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class SwaggerConfigTest {

    @MockBean
    private FileStorageService storageServiceMock;

    @MockBean
    private InstitutionService institutionServiceMock;

    @MockBean
    private InstitutionV2Service institutionV2Service;

    @MockBean
    private ExchangeTokenServiceV2 exchangeTokenServiceV2Mock;

    @MockBean
    private ProductService productServiceMock;

    @MockBean
    private it.pagopa.selfcare.product.service.ProductService productServiceMock2;

    @MockBean
    private UserV2Service userServiceV2Mock;

    @MockBean
    private BrokerService brokerService;

    @MockBean
    private DelegationService delegationServiceMock;

    @MockBean
    private SupportService supportServiceMock;

    @MockBean
    private DelegationMapper delegationMapperImpl;

    @MockBean
    private SupportMapper supportMapperImpl;

    @MockBean
    private UserMapperV2 userMapperImpl;

    @MockBean
    private UserMapper userMapper;

    @MockBean
    private GroupMapper groupMapper;

    @MockBean
    private GroupMapperV2 groupMapperImpl;

    @MockBean
    private UserGroupV2Service userGroupServiceV2Mock;

    @MockBean
    private InstitutionResourceMapper institutionResourceMapper;

    @MockBean
    private OnboardingMapper onboardingMapper;

    @Autowired
    WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void swaggerSpringPlugin() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        mockMvc.perform(MockMvcRequestBuilders.get("/v3/api-docs").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                .andDo(result -> {
                    assertNotNull(result);
                    assertNotNull(result.getResponse());
                    String content = result.getResponse().getContentAsString();
                    assertFalse(content.isBlank());
                    Object swagger = objectMapper.readValue(result.getResponse().getContentAsString(), Object.class);
                    String formatted = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(swagger);
                    Path basePath = Paths.get("src/main/resources/swagger/");
                    Files.createDirectories(basePath);
                    Files.write(basePath.resolve("api-docs.json"), formatted.getBytes());
                    content = content.replace("${openapi_title}", "openapi"); //ignore ${openapi_title} placeholder
                    assertFalse(content.contains("${"), "Generated swagger contains placeholders");
                });
    }

}