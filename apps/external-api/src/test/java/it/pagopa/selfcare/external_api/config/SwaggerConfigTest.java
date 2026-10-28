package it.pagopa.selfcare.external_api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import feign.Client;
import feign.Request;
import feign.Response;
import it.pagopa.selfcare.commons.tenant.TenantContext;
import it.pagopa.selfcare.external_api.client.MsCoreRestClient;
import it.pagopa.selfcare.external_api.client.MsProductApiClient;
import it.pagopa.selfcare.external_api.client.UserRegistryRestClient;
import it.pagopa.selfcare.external_api.mapper.*;
import it.pagopa.selfcare.external_api.service.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import jakarta.servlet.Filter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Slf4j
class SwaggerConfigTest {

    private static final KeyPair AR = keyPair();
    private static final KeyPair PNPG = keyPair();

    @DynamicPropertySource
    static void tenantProperties(DynamicPropertyRegistry properties) {
        properties.add("TENANT_SUPPORTED_TENANTS", () -> "AR,PNPG");
        properties.add("JWT_TEST_AR", () -> Base64.getEncoder().encodeToString(AR.getPublic().getEncoded()));
        properties.add("JWT_TEST_PNPG", () -> Base64.getEncoder().encodeToString(PNPG.getPublic().getEncoded()));
        properties.add("USER_REGISTRY_API_KEY_AR", () -> "ar-test-key");
        properties.add("USER_REGISTRY_API_KEY_PNPG", () -> "pnpg-test-key");
        properties.add("SELFCARE_TENANT_DATA_ISOLATION", () -> """
                {"AR":{"jwt":{"publicKeyEnvVar":"JWT_TEST_AR","issuers":["SPID","PAGOPA"],"audiences":["AR-audience"]},
                  "userRegistry":{"apiKeyEnvVar":"USER_REGISTRY_API_KEY_AR"}},
                 "PNPG":{"jwt":{"publicKeyEnvVar":"JWT_TEST_PNPG","issuers":["SPID"],"audiences":["PNPG-audience"]},
                  "userRegistry":{"apiKeyEnvVar":"USER_REGISTRY_API_KEY_PNPG"}}}
                """);
    }

    @Autowired
    private TenantContext tenantContext;

    @MockBean
    private Client httpTransport;

    @Autowired
    private MsCoreRestClient institutionClient;

    @Autowired
    private MsProductApiClient productClient;

    @Autowired
    private UserRegistryRestClient userRegistryClient;

    @MockBean
    private InstitutionService institutionService;

    @MockBean
    private InstitutionResourceMapper institutionResourceMapper;

    @MockBean
    private ProductsMapper productsMapper;

    @MockBean
    private UserInfoResourceMapper userInfoResourceMapper;

    @MockBean
    private NationalRegistryMapper nationalRegistryMapper;

    @MockBean
    private OnboardingResourceMapper onboardingResourceMapper;

    @MockBean
    private TokenResourceMapper tokenResourceMapper;

    @MockBean
    private OnboardingService onboardingService;

    @MockBean
    private ContractService contractService;

    @MockBean
    private UserService userService;

    @MockBean
    private TokenService tokenService;

    @MockBean
    private ProductMsService productMsService;

    @Autowired
    WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void swaggerSpringPlugin() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        mockMvc.perform(MockMvcRequestBuilders.get("/v3/api-docs").accept(MediaType.APPLICATION_JSON)).andExpect(MockMvcResultMatchers.status().is2xxSuccessful()).andDo((result) -> {
            assertNotNull(result);
            assertNotNull(result.getResponse());
            final String content = result.getResponse().getContentAsString();
            checkPlaceholders(content);
            assertFalse(content.isBlank());
            assertFalse(content.contains("${"), "Generated swagger contains placeholders");
            Object swagger = objectMapper.readValue(result.getResponse().getContentAsString(), Object.class);
            String formatted = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(swagger);
            Path basePath = Paths.get("src/main/resources/swagger/");
            Files.createDirectories(basePath);
            Files.write(basePath.resolve("api-docs.json"), formatted.getBytes());
        });
    }

    @Test
    void interleavedRequestsUseVerifiedTenantAndClearRequestContexts() throws Exception {
        MockMvc mvc = securedMvc();
        for (String tenant : List.of("AR", "PNPG", "AR")) {
            doAnswer(ignored -> {
                assertEquals(tenant, tenantContext.requiredTenantId());
                return List.of();
            }).when(institutionService).getInstitutionUserProductsV2("institution", "user");
            mvc.perform(productsRequest()
                            .header("X-Tenant-Id", tenant.toLowerCase(java.util.Locale.ROOT))
                            .header("Authorization", "Bearer " + token(tenant, tenant.equals("AR") ? AR : PNPG)
                                    .setIssuer(tenant.equals("AR") ? "PAGOPA" : "SPID").compact()))
                    .andExpect(status().isOk());
            assertTrue(tenantContext.tenantId().isEmpty());
            assertNull(SecurityContextHolder.getContext().getAuthentication());
        }
    }

    @Test
    void rejectsMissingDuplicatedUnknownMismatchedAndUnclaimedTenants() throws Exception {
        MockMvc mvc = securedMvc();
        String arToken = token("AR", AR).compact();
        for (String[] tenants : new String[][] {{}, {"AR", "AR"}, {"UNKNOWN"}, {"PNPG"}}) {
            var request = productsRequest().header("Authorization", "Bearer " + arToken);
            if (tenants.length > 0) {
                request.header("X-Tenant-Id", (Object[]) tenants);
            }
            mvc.perform(request).andExpect(status().is4xxClientError());
            assertTrue(tenantContext.tenantId().isEmpty());
        }
        mvc.perform(productsRequest().header("X-Tenant-Id", "PNPG")
                        .header("Authorization", "Bearer " + token(null, PNPG).compact()))
                .andExpect(status().is4xxClientError());
        assertTrue(tenantContext.tenantId().isEmpty());
        verifyNoInteractions(institutionService);
    }

    @Test
    void rejectsWrongTenantKeysInvalidTrustAndExpiredOrPrematureTokens() throws Exception {
        for (String invalid : List.of(
                token("PNPG", AR).compact(),
                token("PNPG", PNPG).setIssuer("PAGOPA").compact(),
                token("PNPG", PNPG).setAudience("AR-audience").compact(),
                token("PNPG", PNPG).setExpiration(Date.from(Instant.now().minusSeconds(300))).compact(),
                token("PNPG", PNPG).setNotBefore(Date.from(Instant.now().plusSeconds(300))).compact(),
                token("PNPG", PNPG).setExpiration(null).compact())) {
            securedMvc().perform(productsRequest().header("X-Tenant-Id", "PNPG")
                            .header("Authorization", "Bearer " + invalid))
                    .andExpect(status().is4xxClientError());
            assertTrue(tenantContext.tenantId().isEmpty());
            assertNull(SecurityContextHolder.getContext().getAuthentication());
        }
        verifyNoInteractions(institutionService);
    }

    @Test
    void serviceFailuresDoNotLeakTenantOrAuthentication() throws Exception {
        doThrow(new IllegalStateException("test failure"))
                .when(institutionService).getInstitutionUserProductsV2("institution", "user");
        securedMvc().perform(productsRequest().header("X-Tenant-Id", "AR")
                        .header("Authorization", "Bearer " + token("AR", AR).compact()))
                .andExpect(status().is5xxServerError());
        assertTrue(tenantContext.tenantId().isEmpty());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void actualFeignRequestsIsolateUserRegistryCredentialsFromInternalAuthentication() throws Exception {
        when(httpTransport.execute(any(), any())).thenAnswer(invocation -> Response.builder()
                .request(invocation.getArgument(0, Request.class)).status(200).reason("OK")
                .headers(Map.of("Content-Type", List.of("application/json")))
                .body("{}", StandardCharsets.UTF_8).build());
        MockHttpServletRequest rawRequest = new MockHttpServletRequest();
        rawRequest.addHeader("X-Tenant-Id", "UNVERIFIED");
        rawRequest.addHeader("Authorization", "Bearer unverified-token");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(rawRequest));
        try {
            for (String tenant : List.of("AR", "PNPG", "AR")) {
                clearInvocations(httpTransport);
                tenantContext.setTenantId(tenant);
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken("test", "verified-" + tenant, List.of()));
                institutionClient.getInstitution("institution");
                productClient._getProductById("product", null);
                userRegistryClient.getUserByInternalId(UUID.randomUUID(),
                        EnumSet.noneOf(it.pagopa.selfcare.external_api.model.user.User.Fields.class));

                ArgumentCaptor<Request> requests = ArgumentCaptor.forClass(Request.class);
                verify(httpTransport, times(3)).execute(requests.capture(), any());
                for (Request internal : requests.getAllValues().subList(0, 2)) {
                    assertEquals(List.of(tenant), List.copyOf(internal.headers().get("X-Tenant-Id")));
                    assertEquals(List.of("Bearer verified-" + tenant),
                            List.copyOf(internal.headers().get("Authorization")));
                    assertFalse(internal.headers().containsKey("x-api-key"));
                    assertFalse(internal.url().contains("tenantId="));
                }
                Request vault = requests.getAllValues().get(2);
                assertEquals(List.of(tenant.equals("AR") ? "ar-test-key" : "pnpg-test-key"),
                        List.copyOf(vault.headers().get("x-api-key")));
                assertFalse(vault.headers().containsKey("Authorization"));
                assertFalse(vault.headers().containsKey("X-Tenant-Id"));
            }
            tenantContext.clear();
            assertThrows(IllegalStateException.class,
                    () -> userRegistryClient.getUserByInternalId(UUID.randomUUID(), null));
            assertThrows(IllegalStateException.class, () -> institutionClient.getInstitution("institution"));
        } finally {
            tenantContext.clear();
            SecurityContextHolder.clearContext();
            RequestContextHolder.resetRequestAttributes();
        }
    }

    private MockMvc securedMvc() {
        return MockMvcBuilders.webAppContextSetup(context)
                .addFilters(context.getBean("springSecurityFilterChain", Filter.class)).build();
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder productsRequest() {
        return MockMvcRequestBuilders.get("/v2/institutions/institution/products").param("userId", "user");
    }

    private static JwtBuilder token(String tenant, KeyPair key) {
        return Jwts.builder().setIssuer("SPID").setAudience((tenant == null ? "PNPG" : tenant) + "-audience")
                .setSubject("test-user").claim("uid", "test-user").claim("tenant_id", tenant)
                .setExpiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(key.getPrivate(), SignatureAlgorithm.RS256);
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

    private static void checkPlaceholders(String content) {
        Pattern pattern = Pattern.compile("\\$\\{(.*?)\\}");
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            log.error("\033[31m An error occurred with placeholder: {}", matcher.group(1));
        }
    }
}
