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
    void updateExpiredOnboardings_OneExpiredOnboarding_UpdatesStatus() {

        when(azureSearchRestClient.searchOnboarding(
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyBoolean(),
                anyInt(),
                anyInt(),
                anyString()))
                .thenReturn(buildResponse(List.of(buildDocument("ONB-1"))));

        service.updateExpiredOnboardings();

        ArgumentCaptor<SearchServiceIndexRequest> captor =
                ArgumentCaptor.forClass(SearchServiceIndexRequest.class);

        verify(azureSearchRestClient, times(1))
                .index(eq("onboarding-index"), eq("2023-11-01"), captor.capture());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> updates =
                (List<Map<String, Object>>) captor.getValue().getValue();

        assertEquals(1, updates.size());

        Map<String, Object> update = updates.get(0);

        assertEquals("merge", update.get("@search.action"));
        assertEquals("ONB-1", update.get("onboardingId"));
        assertEquals("EXPIRED", update.get("status"));
    }

    @Test
    void updateExpiredOnboardings_MultiplePages_UpdatesAllPages() {

        List<OnboardingSearchDocument> firstPage =
                java.util.stream.IntStream.range(0, 1000)
                        .mapToObj(i -> buildDocument("ONB-" + i))
                        .toList();

        OnboardingSearchResponse secondPage = new OnboardingSearchResponse();

        when(azureSearchRestClient.searchOnboarding(
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyBoolean(),
                anyInt(),
                anyInt(),
                anyString()))
                .thenReturn(buildResponse(firstPage))
                .thenReturn(secondPage);

        service.updateExpiredOnboardings();

        verify(azureSearchRestClient, times(1))
                .index(eq("onboarding-index"), eq("2023-11-01"), any(SearchServiceIndexRequest.class));

        verify(azureSearchRestClient, times(2))
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