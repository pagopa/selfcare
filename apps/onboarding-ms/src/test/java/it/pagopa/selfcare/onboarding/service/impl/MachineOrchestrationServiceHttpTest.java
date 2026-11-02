package it.pagopa.selfcare.onboarding.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import it.pagopa.selfcare.m2m.MachineTokenClient;
import it.pagopa.selfcare.onboarding.service.OrchestrationService;
import it.pagopa.selfcare.onboarding.support.OnboardingFunctionsHttpContractResource;
import it.pagopa.selfcare.onboarding.support.ProductHttpContractProfile;
import it.pagopa.selfcare.tenant.TenantContext;
import jakarta.inject.Inject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(MachineOrchestrationServiceHttpTest.Profile.class)
@QuarkusTestResource(value = OnboardingFunctionsHttpContractResource.class, restrictToAnnotatedClass = true)
class MachineOrchestrationServiceHttpTest {
  @InjectMock MachineTokenClient tokens;
  @Inject OrchestrationService orchestration;
  @Inject TenantContext tenant;
  private final HttpClient http = HttpClient.newHttpClient();
  private URI requests;

  public static class Profile extends ProductHttpContractProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
      Map<String, String> values = new HashMap<>(super.getConfigOverrides());
      values.put("selfcare.m2m.enabled", "true");
      values.put("selfcare.m2m.endpoint", "https://auth.invalid/token/service");
      values.put("selfcare.m2m.entra-scope", "api://auth/.default");
      values.put("selfcare.m2m.audience", "onboarding-functions");
      values.put("selfcare.m2m.scope", "onboarding-functions.invoke");
      return values;
    }
  }

  @BeforeEach
  void setUp() throws Exception {
    requests = URI.create(ConfigProvider.getConfig()
        .getValue("onboarding-functions-http-contract.url", String.class) + "/_requests");
    http.send(HttpRequest.newBuilder(requests).DELETE().build(), HttpResponse.BodyHandlers.discarding());
    tenant.setTenantId("AR");
    when(tokens.isEnabled()).thenReturn(true);
  }

  @Test
  void backgroundCallUsesMachineCredentialAndPreservesTenantAndFunctionKey() throws Exception {
    CompletableFuture<String> acquired = new CompletableFuture<>();
    when(tokens.token("AR", "onboarding-functions", "onboarding-functions.invoke")).thenReturn(acquired);
    var pending = orchestration.triggerOrchestrationIfEnabled("onb-machine", null, "m2m:onboarding-ms")
        .subscribeAsCompletionStage();
    acquired.complete("machine-jwt");
    pending.toCompletableFuture().get(10, java.util.concurrent.TimeUnit.SECONDS);
    List<Map<String, String>> calls = calls();
    assertEquals(1, calls.size());
    assertEquals("Bearer machine-jwt", calls.get(0).get("authorization"));
    assertEquals("AR", calls.get(0).get("tenant"));
    assertFalse(calls.get(0).get("functionKey").isBlank());
    verify(tokens).token("AR", "onboarding-functions", "onboarding-functions.invoke");
  }

  @Test
  void issuerFailureDoesNotSendLegacyOrUnauthenticatedRequest() throws Exception {
    when(tokens.token("AR", "onboarding-functions", "onboarding-functions.invoke"))
        .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("Issuer unavailable")));
    assertThrows(RuntimeException.class, () ->
        orchestration.triggerOrchestrationIfEnabled("onb-machine", null, "m2m:onboarding-ms")
            .await().atMost(Duration.ofSeconds(10)));
    assertTrue(calls().isEmpty());
  }

  private List<Map<String, String>> calls() throws Exception {
    String body = http.send(HttpRequest.newBuilder(requests).GET().build(),
        HttpResponse.BodyHandlers.ofString()).body();
    return new ObjectMapper().readValue(body, new TypeReference<>() {});
  }
}
