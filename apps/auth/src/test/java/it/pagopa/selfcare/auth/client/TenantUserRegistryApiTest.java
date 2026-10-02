package it.pagopa.selfcare.auth.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.tenant.TenantContext;
import jakarta.inject.Inject;
import java.util.concurrent.TimeUnit;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.openapi.quarkus.user_registry_json.model.SaveUserDto;

@QuarkusTest
@QuarkusTestResource(value = EmbeddedUserRegistryResource.class, restrictToAnnotatedClass = true)
class TenantUserRegistryApiTest {

  @Inject TenantContext tenantContext;
  @Inject @RestClient TenantUserRegistryApi client;

  @Test
  void generatedClientSendsOnlyTheCurrentTenantsApiKey() throws InterruptedException {
    tenantContext.setTenantId("AR");
    client.saveUsingPATCH(new SaveUserDto()).await().indefinitely();
    assertEquals("123", EmbeddedUserRegistryResource.RECEIVED_KEYS.poll(5, TimeUnit.SECONDS));

    tenantContext.setTenantId("PNPG");
    client.saveUsingPATCH(new SaveUserDto()).await().indefinitely();
    assertEquals("456", EmbeddedUserRegistryResource.RECEIVED_KEYS.poll(5, TimeUnit.SECONDS));
  }
}
