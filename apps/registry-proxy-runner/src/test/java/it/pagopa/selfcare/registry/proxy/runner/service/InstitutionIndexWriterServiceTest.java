package it.pagopa.selfcare.registry.proxy.runner.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import it.pagopa.selfcare.registry.proxy.runner.client.AzureSearchRestClient;
import it.pagopa.selfcare.registry.proxy.runner.model.IpaInstitution;
import it.pagopa.selfcare.registry.proxy.runner.model.IpaInstitutionIndex;
import it.pagopa.selfcare.registry.proxy.runner.model.SearchServiceIndexRequest;
import it.pagopa.selfcare.registry.proxy.runner.model.SearchServiceIndexResponse;
import java.util.List;
import java.util.Map;
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

  private SearchServiceIndexResponse buildFetchAllResponse(List<IpaInstitutionIndex> items) {
    SearchServiceIndexResponse response = new SearchServiceIndexResponse();
    response.setValue(items);
    return response;
  }

  private IpaInstitutionIndex buildIndexDoc(String id, String updateDate) {
    IpaInstitutionIndex idx = new IpaInstitutionIndex();
    idx.setId(id);
    idx.setUpdateDate(updateDate);
    return idx;
  }

  private IpaInstitution buildInstitution(String taxCode, String updateDate) {
    IpaInstitution inst = new IpaInstitution();
    inst.setTaxCode(taxCode);
    inst.setUpdateDate(updateDate);
    return inst;
  }

  @Test
  void testIndex_UpdatesNeeded() {
    // Index has ID1 with old date, ID2 with same date
    when(azureSearchRestClient.search(
            eq("ipa-inst-idx"), eq("2023-11-01"), any(), any(), eq(true), eq(1000), eq(0), any()))
        .thenReturn(
            buildFetchAllResponse(
                List.of(buildIndexDoc("ID1", "2023-01-01"), buildIndexDoc("ID2", "2023-01-01"))));

    IpaInstitution inst1 = buildInstitution("ID1", "2023-01-02");
    IpaInstitution inst2 = buildInstitution("ID2", "2023-01-01");

    service.index(List.of(inst1, inst2));

    // only inst1 should be indexed (updated), no deletes
    ArgumentCaptor<SearchServiceIndexRequest> captor =
        ArgumentCaptor.forClass(SearchServiceIndexRequest.class);
    verify(azureSearchRestClient, times(1))
        .index(eq("ipa-inst-idx"), eq("2023-11-01"), captor.capture());

    List<?> values = captor.getValue().getValue();
    assertEquals(1, values.size());
    assertEquals("ID1", ((IpaInstitutionIndex) values.get(0)).getId());
  }

  @Test
  void testIndex_DeletesStaleDocuments() {
    // Index has ID1, ID2, ID3 but source only has ID1
    when(azureSearchRestClient.search(
            eq("ipa-inst-idx"), eq("2023-11-01"), any(), any(), eq(true), eq(1000), eq(0), any()))
        .thenReturn(
            buildFetchAllResponse(
                List.of(
                    buildIndexDoc("ID1", "2023-01-01"),
                    buildIndexDoc("ID2", "2023-01-01"),
                    buildIndexDoc("ID3", "2023-01-01"))));

    IpaInstitution inst1 = buildInstitution("ID1", "2023-01-01");

    service.index(List.of(inst1));

    // First call: no upserts needed (up to date). Only delete call expected.
    ArgumentCaptor<SearchServiceIndexRequest> captor =
        ArgumentCaptor.forClass(SearchServiceIndexRequest.class);
    verify(azureSearchRestClient, times(1))
        .index(eq("ipa-inst-idx"), eq("2023-11-01"), captor.capture());

    SearchServiceIndexRequest deleteRequest = captor.getValue();
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> deleteBatch = (List<Map<String, Object>>) deleteRequest.getValue();
    assertEquals(2, deleteBatch.size());
    assertTrue(deleteBatch.stream().allMatch(doc -> "delete".equals(doc.get("@search.action"))));
    assertTrue(
        deleteBatch.stream()
            .map(doc -> (String) doc.get("id"))
            .allMatch(id -> id.equals("ID2") || id.equals("ID3")));
  }

  @Test
  void testIndex_DeletesAndUpdates() {
    // Index has ID1 (stale date), ID2 (to delete). Source has ID1 (updated).
    when(azureSearchRestClient.search(
            eq("ipa-inst-idx"), eq("2023-11-01"), any(), any(), eq(true), eq(1000), eq(0), any()))
        .thenReturn(
            buildFetchAllResponse(
                List.of(buildIndexDoc("ID1", "2023-01-01"), buildIndexDoc("ID2", "2023-01-01"))));

    IpaInstitution inst1 = buildInstitution("ID1", "2023-01-02");

    service.index(List.of(inst1));

    // Two calls to index: one for upsert, one for delete
    ArgumentCaptor<SearchServiceIndexRequest> captor =
        ArgumentCaptor.forClass(SearchServiceIndexRequest.class);
    verify(azureSearchRestClient, times(2))
        .index(eq("ipa-inst-idx"), eq("2023-11-01"), captor.capture());

    List<SearchServiceIndexRequest> allRequests = captor.getAllValues();

    // First call: upsert ID1
    List<?> upsertValues = allRequests.get(0).getValue();
    assertEquals(1, upsertValues.size());
    assertEquals("ID1", ((IpaInstitutionIndex) upsertValues.get(0)).getId());

    // Second call: delete ID2
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> deleteValues =
        (List<Map<String, Object>>) allRequests.get(1).getValue();
    assertEquals(1, deleteValues.size());
    assertEquals("delete", deleteValues.get(0).get("@search.action"));
    assertEquals("ID2", deleteValues.get(0).get("id"));
  }

  @Test
  void testIndex_NoDeletes_WhenAllSourceItemsPresent() {
    // Index has ID1, source has ID1 with same date => no action at all
    when(azureSearchRestClient.search(
            eq("ipa-inst-idx"), eq("2023-11-01"), any(), any(), eq(true), eq(1000), eq(0), any()))
        .thenReturn(buildFetchAllResponse(List.of(buildIndexDoc("ID1", "2023-01-01"))));

    IpaInstitution inst1 = buildInstitution("ID1", "2023-01-01");

    service.index(List.of(inst1));

    // No index calls (no updates, no deletes)
    verify(azureSearchRestClient, never())
        .index(any(), any(), any(SearchServiceIndexRequest.class));
  }

  @Test
  void testIndex_EmptyIndex_NoDeletes() {
    // Index is empty, source has items => only upsert, no delete
    when(azureSearchRestClient.search(
            eq("ipa-inst-idx"), eq("2023-11-01"), any(), any(), eq(true), eq(1000), eq(0), any()))
        .thenReturn(new SearchServiceIndexResponse());

    IpaInstitution inst1 = buildInstitution("ID1", "2023-01-01");

    service.index(List.of(inst1));

    // One upsert call
    ArgumentCaptor<SearchServiceIndexRequest> captor =
        ArgumentCaptor.forClass(SearchServiceIndexRequest.class);
    verify(azureSearchRestClient, times(1))
        .index(eq("ipa-inst-idx"), eq("2023-11-01"), captor.capture());

    List<?> values = captor.getValue().getValue();
    assertEquals(1, values.size());
  }
}
