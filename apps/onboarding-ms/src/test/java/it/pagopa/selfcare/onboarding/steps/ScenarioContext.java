package it.pagopa.selfcare.onboarding.steps;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;

@ApplicationScoped
@Data
public class ScenarioContext {

  @Inject IntegrationOnboardingResources onboardingResourcesFile;

  private Map<String, String> requestBodies = new HashMap<>();
  private String currentKey;

  public void storeRequestBody(String key) {
    getRequestBodies().put(key, getOnboardingResourcesFile().getJsonTemplate(key));
    setCurrentKey(key);
  }

  public String getCurrentRequestBody() {
    return getCurrentKey() != null ? getRequestBodies().get(getCurrentKey()) : null;
  }
}
