package it.pagopa.selfcare.party.registry_proxy.connector.rest.config;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.commons.tenant.*;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.PdndProfile;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class RegistryProxyTenantResourcesTest {

  private final ObjectMapper mapper = new ObjectMapper();
  private final TenantContext context = new TenantContext();
  private MockEnvironment environment;

  @BeforeEach
  void configure() {
    environment = new MockEnvironment().withProperty("tenant.mongodb.required", "false")
        .withProperty("jwt.tenant-claim-required", "true");
    for (String tenant : new String[] {"AR", "PNPG"}) {
      for (String key : new String[] {"JWT", "SEARCH", "NATIONAL", "CLIENT", "PRIVATE", "KID", "PURPOSE"}) {
        environment.setProperty(key + "_" + tenant, tenant + "-" + key);
        environment.setProperty("INVITALIA_" + key + "_" + tenant, tenant + "-INVITALIA-" + key);
      }
    }
  }

  @Test
  void interleavedRequestsSelectTheirOwnResourcesAndPdndProfile() throws Exception {
    var resources = resources(configuration(false));
    resources.initialize();
    var resolver = new PdndSecretValueResolver(resources);
    for (String tenant : new String[] {"AR", "PNPG", "AR"}) {
      context.withTenant(tenant, () -> {
        assertEquals("onboarding-" + tenant.toLowerCase(), resources.search().onboardingIndex());
        assertEquals(tenant + "-SEARCH", resources.secret(resources.search().apiKeyEnvVar()));
        assertEquals(tenant + "-NATIONAL", resources.secret(resources.current().nationalRegistries().apiKeyEnvVar()));
        var pdnd = resolver.resolve("other-product");
        assertEquals(tenant + "-CLIENT", pdnd.getClientId());
        assertEquals(tenant + "-PRIVATE", pdnd.getSecretKey());
        assertEquals(PdndProfile.SELFCARE, pdnd.getProfile());
        assertEquals(tenant + "-KID", pdnd.getJwtConfig().getKid());
        return null;
      });
      assertTrue(context.tenantId().isEmpty());
    }
    context.withTenant("AR", () -> {
      var invitalia = resolver.resolve("invitalia-product");
      assertEquals(PdndProfile.INVITALIA, invitalia.getProfile());
      assertEquals("AR-INVITALIA-CLIENT", invitalia.getClientId());
      assertEquals("AR-INVITALIA-PRIVATE", invitalia.getSecretKey());
      assertEquals("AR-INVITALIA-KID", invitalia.getJwtConfig().getKid());
      return null;
    });
  }

  @Test
  void rejectsMissingOrUnknownContextAndUnconfiguredOptionalResource() throws Exception {
    var resources = resources(configuration(true));
    resources.initialize();
    assertThrows(UnresolvedTenantException.class, resources::current);
    context.withTenant("UNKNOWN", () -> {
      assertThrows(UnknownTenantException.class, resources::current);
      return null;
    });
    context.withTenant("PNPG", () -> {
      assertThrows(IllegalStateException.class, resources::search);
      return null;
    });
  }

  @Test
  void rejectsMissingCredentialsAndSharedOnboardingIndexAtStartup() throws Exception {
    String json = configuration(false);
    environment.setProperty("SEARCH_PNPG", "");
    assertThrows(IllegalStateException.class, () -> resources(json).initialize());
    environment.setProperty("SEARCH_PNPG", "pnpg-key");
    assertThrows(IllegalStateException.class,
        () -> resources(json.replace("onboarding-pnpg", "onboarding-ar")).initialize());
  }

  @Test
  void rejectsUntrustedEndpointsAndLiteralCredentials() throws Exception {
    var resources = resources(configuration(false));
    assertThrows(IllegalStateException.class, () -> resources.secret("literal-key"));
    for (String endpoint : new String[] {
        "http://example.com", "https://user@example.com", "https://example.com?key=value"}) {
      assertThrows(IllegalStateException.class, () -> RegistryProxyTenantResources.endpoint(endpoint));
    }
  }

  @Test
  void sharedDeploymentsRequireBoundTokensAndExplicitTrustPolicy() throws Exception {
    String json = configuration(false);
    environment.setProperty("jwt.tenant-claim-required", "false");
    assertThrows(IllegalStateException.class, () -> resources(json).initialize());
    environment.setProperty("jwt.tenant-claim-required", "true");
    assertThrows(IllegalStateException.class,
        () -> resources(json.replace("\"issuers\":[\"SPID\"]", "\"issuers\":[]")).initialize());
    assertThrows(IllegalStateException.class,
        () -> resources(json.replace("\"audiences\":[\"audience\"]", "\"audiences\":[]")).initialize());
  }

  private RegistryProxyTenantResources resources(String json) {
    var registry = new TenantRegistry(mapper, environment, json, "AR,PNPG", "");
    registry.initialize();
    return new RegistryProxyTenantResources(registry, context, environment);
  }

  private String configuration(boolean omitPnpgSearch) throws Exception {
    return mapper.writeValueAsString(Map.of(
        "AR", definition("AR", false),
        "PNPG", definition("PNPG", omitPnpgSearch)));
  }

  private TenantDefinition definition(String tenant, boolean omitSearch) {
    var pdnd = new TenantDefinition.PdndDefinition(
        "CLIENT_" + tenant, "PRIVATE_" + tenant, "KID_" + tenant, "PURPOSE_" + tenant, "audience");
    var invitalia = new TenantDefinition.PdndDefinition(
        "INVITALIA_CLIENT_" + tenant, "INVITALIA_PRIVATE_" + tenant,
        "INVITALIA_KID_" + tenant, "INVITALIA_PURPOSE_" + tenant, "invitalia-audience");
    return new TenantDefinition(null, new TenantDefinition.JwtDefinition(
        "JWT_" + tenant, java.util.Set.of("SPID"), java.util.Set.of("audience")), Map.of(),
        null, null, new TenantDefinition.RegistryProxyDefinition(
            omitSearch ? null : new TenantDefinition.SearchDefinition(
                "https://shared-search.example", "onboarding-" + tenant.toLowerCase(), "SEARCH_" + tenant),
            new TenantDefinition.NationalRegistriesDefinition(
                "https://national-" + tenant.toLowerCase() + ".example", "NATIONAL_" + tenant),
            pdnd, invitalia, java.util.Set.of("invitalia-product")));
  }
}
