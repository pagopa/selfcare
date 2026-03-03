package it.pagopa.selfcare.onboarding.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.core.ServerResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusTest
class InstitutionServiceTest {

  @Inject
  InstitutionService institutionService;

  @RestClient @InjectMock
  org.openapi.quarkus.core_json.api.InstitutionApi institutionApi;

  private final String productId = "productId";
  private final String institutionId = "institutionId";

  @Test
  void deleteInstitution() {
    Response response = new ServerResponse(null, 200, null);
    when(institutionApi.deleteOnboardedInstitutionUsingDELETE(any(), any())).thenReturn(response);
    institutionService.deleteByIdAndProductId(institutionId, productId);

    Mockito.verify(institutionApi, times(1))
            .deleteOnboardedInstitutionUsingDELETE(any(), any());
  }

  @Test
  void deleteInstitutionWithException() {
    Response response = new ServerResponse(null, 500, null);
    when(institutionApi.deleteOnboardedInstitutionUsingDELETE(any(), any())).thenReturn(response);
    assertThrows(RuntimeException.class, () -> institutionService.deleteByIdAndProductId(institutionId, productId));
  }

}
