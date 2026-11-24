package it.pagopa.selfcare.user.integration_test.steps;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.quarkus.arc.Arc;
import it.pagopa.selfcare.cucumber.utils.SharedStepData;
import it.pagopa.selfcare.cucumber.utils.TestDataProvider;
import it.pagopa.selfcare.cucumber.utils.TestJwtGenerator;
import it.pagopa.selfcare.cucumber.utils.model.JwtData;
import it.pagopa.selfcare.tenant.TenantContext;
import it.pagopa.selfcare.tenant.TenantRegistry;
import it.pagopa.selfcare.tenant.mongodb.TenantMongoClientProducer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.time.Duration;
import java.io.IOException;
import java.util.ArrayList;
import org.bson.Document;

@ApplicationScoped
public class MultitenancySteps {
  @Inject TenantContext context;
  @Inject SharedStepData data;
  @Inject TestDataProvider testData;
  @Inject TestJwtGenerator jwt;
  @Inject TenantRegistry registry;
  @Inject TenantMongoClientProducer mongo;
  @Inject com.fasterxml.jackson.databind.ObjectMapper mapper;
  private boolean activatedContext;

  @Before(order = -1000)
  public void initializeScenarioTenant() {
    var requestContext = Arc.container().requestContext();
    activatedContext = !requestContext.isActive();
    if (activatedContext) {
      requestContext.activate();
    }
    context.setTenantId("AR");
  }

  @After(order = -1000)
  public void clearScenarioTenant() {
    context.clear();
    if (activatedContext) {
      Arc.container().requestContext().terminate();
    }
  }

  @Before(value = "@TenantIsolation", order = 10001)
  public void seedIsolatedFixtures() throws IOException {
    for (String tenant : List.of("AR", "PNPG")) {
      String resource = "db/userInstitutions-tenant-" + tenant.toLowerCase(java.util.Locale.ROOT) + ".json";
      try (var stream = getClass().getClassLoader().getResourceAsStream(resource)) {
        if (stream == null) {
          throw new IOException("Missing tenant fixture " + resource);
        }
        var documents = new ArrayList<Document>();
        mapper.readTree(stream).forEach(node -> documents.add(Document.parse(node.toString())));
        mongo.clientForTenant(tenant).getDatabase(registry.resolve(tenant).mongo().database())
            .getCollection("userInstitutions").insertMany(documents).await().atMost(Duration.ofSeconds(5));
      }
    }
  }

  @After(value = "@TenantIsolation", order = 10001)
  public void removeIsolatedFixtures() {
    for (String tenant : List.of("AR", "PNPG")) {
      mongo.clientForTenant(tenant).getDatabase(registry.resolve(tenant).mongo().database())
          .getCollection("userInstitutions").deleteMany(new Document("institutionId",
              new Document("$in", List.of("tenant-isolation-institution", "legacy-pnpg-institution"))))
          .await().atMost(Duration.ofSeconds(5));
    }
  }

  @Given("An authenticated request for tenant {string}")
  public void authenticatedTenant(String tenant) {
    var base = testData.getTestData().getJwtData().get(0);
    var token = new JwtData();
    token.setJwtHeader(new HashMap<>(base.getJwtHeader()));
    token.setJwtPayload(new HashMap<>(base.getJwtPayload()));
    token.getJwtPayload().put("tenant_id", tenant);
    data.setToken(jwt.generateToken(token));
    data.setTenantId(tenant);
  }

  @Given("The tenant header is absent")
  public void removeTenantHeader() {
    data.setTenantId(null);
  }

  @Given("The tenant header is {string}")
  public void changeTenantHeader(String tenant) {
    data.setTenantId(tenant);
  }
}
