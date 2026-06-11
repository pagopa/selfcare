package it.pagopa.selfcare.registry.proxy.runner.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import it.pagopa.selfcare.registry.proxy.runner.client.AzureSearchRestClient;
import it.pagopa.selfcare.registry.proxy.runner.model.OnboardingSearchDocument;
import it.pagopa.selfcare.registry.proxy.runner.model.OnboardingSearchResponse;
import it.pagopa.selfcare.registry.proxy.runner.model.SearchServiceIndexRequest;
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
class OnboardingStatusUpdateServiceTest {

    @Mock
    AzureSearchRestClient azureSearchRestClient;

    @InjectMocks
    OnboardingStatusUpdateService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "indexName", "onboarding-index");
        ReflectionTestUtils.setField(service, "apiVersion", "2023-11-01");
    }

    private OnboardingSearchDocument buildDocument(String onboardingId) {
        OnboardingSearchDocument doc = new OnboardingSearchDocument();
        doc.setOnboardingId(onboardingId);
        return doc;
    }

    private OnboardingSearchResponse buildResponse(List<OnboardingSearchDocument> docs) {
        OnboardingSearchResponse response = new OnboardingSearchResponse();
        response.setValue(docs);
        return response;
    }

    @Test
    void updateExpiredOnboardings_NoResults_NoUpdates() {

        when(azureSearchRestClient.searchOnboarding(
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyBoolean(),
                anyInt(),
                anyInt(),
                anyString()))
                .thenReturn(new OnboardingSearchResponse());

        service.updateExpiredOnboardings();

        verify(azureSearchRestClient, never())
                .index(anyString(), anyString(), any(SearchServiceIndexRequest.class));
    }

    @Test
    void updateExpiredOnboardings_NullResponse_NoUpdates() {

        when(azureSearchRestClient.searchOnboarding(
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyBoolean(),
                anyInt(),
                anyInt(),
                anyString()))
                .thenReturn(null);

        service.updateExpiredOnboardings();

        verify(azureSearchRestClient, never())
                .index(anyString(), anyString(), any(SearchServiceIndexRequest.class));
    }

    @Test
    void updateExpiredOnboardings_OneBatch_UpdatesStatus() {

        when(azureSearchRestClient.searchOnboarding(
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyBoolean(),
                anyInt(),
                anyInt(),
                anyString()))
                .thenReturn(buildResponse(List.of(buildDocument("ONB-1"))))
                .thenReturn(new OnboardingSearchResponse()); // stop condition

        service.updateExpiredOnboardings();

        ArgumentCaptor<SearchServiceIndexRequest> captor =
                ArgumentCaptor.forClass(SearchServiceIndexRequest.class);

        verify(azureSearchRestClient, times(1))
                .index(eq("onboarding-index"), eq("2023-11-01"), captor.capture());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> updates =
                (List<Map<String, Object>>) captor.getValue().getValue();

        assertEquals(1, updates.size());
        assertEquals("ONB-1", updates.get(0).get("onboardingId"));
        assertEquals("EXPIRED", updates.get(0).get("status"));
        assertEquals("merge", updates.get(0).get("@search.action"));
    }

    @Test
    void updateExpiredOnboardings_MultipleIterations_StopsOnEmpty() {

        List<OnboardingSearchDocument> firstBatch =
                List.of(buildDocument("ONB-1"), buildDocument("ONB-2"));

        when(azureSearchRestClient.searchOnboarding(
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyBoolean(),
                anyInt(),
                anyInt(),
                anyString()))
                .thenReturn(buildResponse(firstBatch))   // 1st loop
                .thenReturn(buildResponse(firstBatch))   // 2nd loop (still data)
                .thenReturn(new OnboardingSearchResponse()); // stop

        service.updateExpiredOnboardings();

        // 2 update calls (one per loop iteration with data)
        verify(azureSearchRestClient, times(2))
                .index(anyString(), anyString(), any(SearchServiceIndexRequest.class));

        // 3 searches: data + data + empty
        verify(azureSearchRestClient, times(3))
                .searchOnboarding(
                        anyString(),
                        anyString(),
                        anyString(),
                        anyString(),
                        anyBoolean(),
                        anyInt(),
                        anyInt(),
                        anyString());
    }
}