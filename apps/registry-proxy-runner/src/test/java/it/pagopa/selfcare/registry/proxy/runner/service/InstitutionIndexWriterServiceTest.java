package it.pagopa.selfcare.registry.proxy.runner.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import it.pagopa.selfcare.registry.proxy.runner.client.AzureSearchRestClient;
import it.pagopa.selfcare.registry.proxy.runner.model.IpaInstitution;
import it.pagopa.selfcare.registry.proxy.runner.model.IpaInstitutionIndex;
import it.pagopa.selfcare.registry.proxy.runner.model.SearchServiceIndexRequest;
import it.pagopa.selfcare.registry.proxy.runner.model.SearchServiceIndexResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class InstitutionIndexWriterServiceTest {

  @Mock AzureSearchRestClient azureSearchRestClient;

  @InjectMocks InstitutionIndexWriterService service;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(service, "indexName", "ipa-inst-idx");
    ReflectionTestUtils.setField(service, "apiVersion", "2023-11-01");
  }

  @Test
  void testIndex_UpdatesNeeded() {
    IpaInstitution inst1 = new IpaInstitution();
    inst1.setTaxCode("ID1");
    inst1.setUpdateDate("2023-01-02");

    IpaInstitution inst2 = new IpaInstitution();
    inst2.setTaxCode("ID2");
    inst2.setUpdateDate("2023-01-01");

    // mock fetch update dates
    SearchServiceIndexResponse resp1 = new SearchServiceIndexResponse();
    IpaInstitutionIndex idx1 = new IpaInstitutionIndex();
    idx1.setUpdateDate("2023-01-01");
    resp1.setValue(List.of(idx1));

    SearchServiceIndexResponse resp2 = new SearchServiceIndexResponse();
    IpaInstitutionIndex idx2 = new IpaInstitutionIndex();
    idx2.setUpdateDate("2023-01-01");
    resp2.setValue(List.of(idx2));

    when(azureSearchRestClient.search(
            eq("ipa-inst-idx"),
            eq("2023-11-01"),
            any(),
            any(),
            eq(false),
            eq(1),
            eq(0),
            eq("id eq 'ID1'")))
        .thenReturn(resp1);
    when(azureSearchRestClient.search(
            eq("ipa-inst-idx"),
            eq("2023-11-01"),
            any(),
            any(),
            eq(false),
            eq(1),
            eq(0),
            eq("id eq 'ID2'")))
        .thenReturn(resp2);

    service.index(List.of(inst1, inst2));

    // only inst1 should be indexed because its date differs
    ArgumentCaptor<SearchServiceIndexRequest> captor =
        ArgumentCaptor.forClass(SearchServiceIndexRequest.class);
    verify(azureSearchRestClient, times(1))
        .index(eq("ipa-inst-idx"), eq("2023-11-01"), captor.capture());

    List<?> values = captor.getValue().getValue();
    assertEquals(1, values.size());
    assertEquals("ID1", ((IpaInstitutionIndex) values.get(0)).getId());
  }
}
