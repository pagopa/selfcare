package it.pagopa.selfcare.auth.service;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntityBase;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheQuery;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.auth.context.AuthTenantContext;
import it.pagopa.selfcare.auth.entity.OtpFlow;
import it.pagopa.selfcare.auth.exception.ResourceNotFoundException;
import it.pagopa.selfcare.auth.model.OtpStatus;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.bson.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@QuarkusTest
@TestProfile(OtpStrictDataIsolationTest.StrictProfile.class)
class OtpStrictDataIsolationTest {

  public static class StrictProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
      return Map.of("selfcare.tenant.strict-data-isolation", "true");
    }
  }

  @InjectMock AuthTenantContext tenantContext;
  @Inject OtpFlowService otpFlowService;

  @Test
  void strictModeExcludesArLegacyRowsFromAllOtpReads() {
    when(tenantContext.getTenantId()).thenReturn("AR");
    PanacheMock.mock(OtpFlow.class);
    ReactivePanacheQuery<ReactivePanacheMongoEntityBase> query = Mockito.mock(ReactivePanacheQuery.class);
    List<Document> filters = new ArrayList<>();
    when(OtpFlow.find(any(Document.class), any(Document.class))).thenAnswer(invocation -> {
      filters.add(invocation.getArgument(0));
      return query;
    });
    when(OtpFlow.find(any(Document.class))).thenAnswer(invocation -> {
      filters.add(invocation.getArgument(0));
      return query;
    });
    when(query.firstResult()).thenReturn(Uni.createFrom().nullItem());
    when(query.firstResultOptional()).thenReturn(Uni.createFrom().item(Optional.empty()));
    when(query.list()).thenReturn(Uni.createFrom().item(List.of()));

    otpFlowService.findLastOtpFlowByUserId("shared-user").await().indefinitely();
    otpFlowService.getOtpInfo("shared-user", OtpStatus.PENDING).await().indefinitely();
    Assertions.assertThrows(ResourceNotFoundException.class,
        () -> otpFlowService.getOtpMailInfo("shared-request").await().indefinitely());
    Assertions.assertThrows(ResourceNotFoundException.class,
        () -> otpFlowService.verifyOtp("shared-uuid", "123456").await().indefinitely());

    Assertions.assertEquals(4, filters.size());
    filters.forEach(filter -> {
      Assertions.assertEquals("AR", filter.getString("tenantId"));
      Assertions.assertFalse(filter.containsKey("$and"));
    });
  }
}
