package it.pagopa.selfcare.party.registry_proxy.web.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import it.pagopa.selfcare.commons.tenant.TenantContext;
import feign.Client;
import feign.Request;
import feign.Response;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.client.OnboardingSearchRestClient;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.client.AzureSearchRestClient;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.client.NationalRegistriesRestClient;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.client.InstitutionRestClient;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.AdELegalRequestBodyDto;
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
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.mockito.ArgumentCaptor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT)
@ComponentScan(basePackages = {"it.pagopa.selfcare.party.registry_proxy.web.controller", "it.pagopa.selfcare.party.registry_proxy.web.model.mapper"})
@TestPropertySource(locations = "classpath:config/application.yml", properties = {"ivass.file.connector.type=rest"})
class SwaggerConfigTest {

    private static final KeyPair AR = keyPair();
    private static final KeyPair PNPG = keyPair();

    @DynamicPropertySource
    static void tenantProperties(DynamicPropertyRegistry properties) {
        properties.add("tenant.mongodb.required", () -> "false");
        properties.add("tenant.supported-tenants", () -> "AR,PNPG");
        properties.add("jwt.tenant-claim-required", () -> "true");
        properties.add("JWT_TEST_AR", () -> Base64.getEncoder().encodeToString(AR.getPublic().getEncoded()));
        properties.add("JWT_TEST_PNPG", () -> Base64.getEncoder().encodeToString(PNPG.getPublic().getEncoded()));
        properties.add("AZURE_SEARCH_API_KEY", () -> "public-search-key");
        properties.add("tenant.registry.json", () -> """
            {"AR":{"jwt":{"publicKeyEnvVar":"JWT_TEST_AR","issuers":["SPID"],"audiences":["test-audience"]},"registryProxy":{}},
             "PNPG":{"jwt":{"publicKeyEnvVar":"JWT_TEST_PNPG","issuers":["SPID"],"audiences":["test-audience"]},"registryProxy":{}}}
            """);
    }

    @Autowired
    private TenantContext tenantContext;

    @MockBean
    private Client httpTransport;

    @Autowired
    private OnboardingSearchRestClient onboardingSearchClient;
    @Autowired
    private AzureSearchRestClient publicSearchClient;
    @Autowired
    private NationalRegistriesRestClient nationalRegistriesClient;
    @Autowired
    private InstitutionRestClient institutionClient;

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

    @MockBean
    private OpenDataLoader openDataLoader;

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

    @Test
    void interleavedTenantRequestsUseVerifiedContextAndClearItAfterwards() throws Exception {
        MockMvc mvc = securedMvc();
        for (String tenant : List.of("AR", "PNPG", "AR")) {
            doAnswer(ignored -> {
                    assertEquals(tenant, tenantContext.requiredTenantId());
                    return List.of();
                }).when(geographicTaxonomiesService).retrieveGeoTaxonomiesByDescription("Rome", null, null);
            mvc.perform(MockMvcRequestBuilders.get("/geotaxonomies")
                    .param("description", "Rome").header("X-Tenant-Id", tenant)
                    .header("Authorization", "Bearer " + token(tenant, tenant.equals("AR") ? AR : PNPG)))
                .andExpect(status().isOk());
            assertTrue(tenantContext.tenantId().isEmpty());
        }
    }

    @Test
    void rejectsMissingDuplicatedUnknownMismatchedAndUnclaimedTenants() throws Exception {
        MockMvc mvc = securedMvc();
        String arToken = token("AR", AR);
        for (String[] headers : new String[][] {{}, {"AR", "AR"}, {"UNKNOWN"}}) {
            var request = MockMvcRequestBuilders.get("/geotaxonomies").param("description", "Rome")
                    .header("Authorization", "Bearer " + arToken);
            if (headers.length > 0) {
                request.header("X-Tenant-Id", (Object[]) headers);
            }
            mvc.perform(request)
                .andExpect(status().is4xxClientError());
            assertTrue(tenantContext.tenantId().isEmpty());
        }
        mvc.perform(MockMvcRequestBuilders.get("/geotaxonomies").param("description", "Rome")
                .header("X-Tenant-Id", "PNPG").header("Authorization", "Bearer " + arToken))
            .andExpect(status().is4xxClientError());
        mvc.perform(MockMvcRequestBuilders.get("/geotaxonomies").param("description", "Rome")
                .header("X-Tenant-Id", "PNPG").header("Authorization", "Bearer " + token(null, PNPG)))
            .andExpect(status().is4xxClientError());
        verifyNoInteractions(geographicTaxonomiesService);
    }

    @Test
    void rejectsClaimsSignedWithAnotherTenantsKeyEvenWhenHeaderMatches() throws Exception {
        securedMvc().perform(MockMvcRequestBuilders.get("/geotaxonomies").param("description", "Rome")
                .header("X-Tenant-Id", "PNPG").header("Authorization", "Bearer " + token("PNPG", AR)))
            .andExpect(status().is4xxClientError());
        assertTrue(tenantContext.tenantId().isEmpty());
        verifyNoInteractions(geographicTaxonomiesService);
    }

    @Test
    void outboundClientsUseOnlyTheirOwnCredentialsAndInternalCallsPropagateTenant() throws Exception {
        when(httpTransport.execute(any(), any())).thenAnswer(invocation -> Response.builder()
                .request(invocation.getArgument(0, Request.class)).status(200).reason("OK")
                .headers(Map.of("Content-Type", List.of("application/json")))
                .body("{}", StandardCharsets.UTF_8).build());
        onboardingSearchClient.searchOnboarding(URI.create("https://tenant-search.example"),
                "tenant-index", "search-secret", "*", "all", null, true, 15L, 0L, "description asc");
        nationalRegistriesClient.verifyLegal(URI.create("https://tenant-national.example/api"),
                "national-secret", new AdELegalRequestBodyDto());
        publicSearchClient.searchIpaInstitutions("*", null, true, 15, 0, null);
        var requests = ArgumentCaptor.forClass(Request.class);
        verify(httpTransport, times(3)).execute(requests.capture(), any());
        Request search = requests.getAllValues().get(0);
        Request national = requests.getAllValues().get(1);
        assertTrue(search.url().startsWith("https://tenant-search.example/indexes/tenant-index/docs?"));
        assertEquals(List.of("search-secret"), List.copyOf(search.headers().get("api-key")));
        assertTrue(national.url().startsWith("https://tenant-national.example/api/agenzia-entrate/legal"));
        assertEquals(List.of("national-secret"), List.copyOf(national.headers().get("x-api-key")));
        assertFalse(search.headers().containsKey("x-api-key"));
        assertFalse(national.headers().containsKey("api-key"));
        Request publicSearch = requests.getAllValues().get(2);
        assertEquals(List.of("public-search-key"), List.copyOf(publicSearch.headers().get("api-key")));
        assertFalse(publicSearch.headers().containsKey("x-api-key"));
        for (Request external : requests.getAllValues()) {
            assertFalse(external.headers().containsKey("Authorization"));
            assertFalse(external.headers().containsKey("X-Tenant-Id"));
        }
        clearInvocations(httpTransport);
        try {
            tenantContext.setTenantId("PNPG");
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken("test", "verified-token", List.of()));
            institutionClient.getById("institution-id");
            verify(httpTransport).execute(requests.capture(), any());
            Request internal = requests.getValue();
            assertEquals(List.of("PNPG"), List.copyOf(internal.headers().get("X-Tenant-Id")));
            assertEquals(List.of("Bearer verified-token"), List.copyOf(internal.headers().get("Authorization")));
            assertFalse(internal.headers().containsKey("api-key"));
            assertFalse(internal.headers().containsKey("x-api-key"));
        } finally {
            tenantContext.clear();
            SecurityContextHolder.clearContext();
        }
        doAnswer(invocation -> Response.builder()
                .request(invocation.getArgument(0, Request.class)).status(503).reason("Unavailable")
                .headers(Map.of()).build()).when(httpTransport).execute(any(), any());
        assertThrows(it.pagopa.selfcare.party.registry_proxy.connector.exception.ServiceUnavailableException.class,
                () -> onboardingSearchClient.searchOnboarding(URI.create("https://tenant-search.example"),
                        "tenant-index", "search-secret", "*", "all", null, true, 15L, 0L, "description asc"));
    }

    private MockMvc securedMvc() {
        return MockMvcBuilders.webAppContextSetup(context)
            .addFilters(context.getBean("springSecurityFilterChain", Filter.class)).build();
    }

    private static String token(String tenant, KeyPair keyPair) {
        return Jwts.builder().setIssuer("SPID").setAudience("test-audience").setSubject("test-user").claim("uid", "test-user")
            .claim("tenant_id", tenant).setExpiration(Date.from(Instant.now().plusSeconds(300)))
            .signWith(keyPair.getPrivate(), SignatureAlgorithm.RS256).compact();
    }

    private static KeyPair keyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
