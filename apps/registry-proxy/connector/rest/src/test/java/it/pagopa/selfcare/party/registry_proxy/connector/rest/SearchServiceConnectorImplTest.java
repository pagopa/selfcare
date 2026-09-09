package it.pagopa.selfcare.party.registry_proxy.connector.rest;

import it.pagopa.selfcare.party.registry_proxy.connector.model.OnboardingIndex;
import it.pagopa.selfcare.party.registry_proxy.connector.model.SearchServiceStatus;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.client.AzureSearchRestClient;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.mapper.SearchServiceMapperImpl;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.search.SearchServiceIndexRequest;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.search.SearchServiceIndexResponse;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.search.SearchServiceOnboardingIndex;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;


@ContextConfiguration(classes = {SearchServiceConnectorImpl.class, SearchServiceMapperImpl.class})
@ExtendWith(SpringExtension.class)
public class SearchServiceConnectorImplTest {

  @Autowired
  private SearchServiceConnectorImpl searchServiceConnector;

  @MockBean
  private AzureSearchRestClient azureSearchRestClient;

  @Test
  void testIndexOnboardingWithOverriddenStatus() {
    when(azureSearchRestClient.indexOnboarding(any())).thenReturn(new SearchServiceStatus());
    OnboardingIndex onboardingIndex = new OnboardingIndex();
    onboardingIndex.setOnboardingId("onboarding-id");
    onboardingIndex.setStatus("OVERRIDDEN");

    searchServiceConnector.indexOnboarding(onboardingIndex);

    ArgumentCaptor<SearchServiceIndexRequest<SearchServiceOnboardingIndex>> requestCaptor =
        ArgumentCaptor.forClass(SearchServiceIndexRequest.class);
    verify(azureSearchRestClient).indexOnboarding(requestCaptor.capture());
    assertEquals("delete", requestCaptor.getValue().getValue().get(0).getAction());
  }

  @Test
  void testIndexOnboardingWithNonOverriddenStatus() {
    when(azureSearchRestClient.indexOnboarding(any())).thenReturn(new SearchServiceStatus());
    OnboardingIndex onboardingIndex = new OnboardingIndex();
    onboardingIndex.setOnboardingId("onboarding-id");
    onboardingIndex.setStatus("ACTIVE");

    searchServiceConnector.indexOnboarding(onboardingIndex);

    ArgumentCaptor<SearchServiceIndexRequest<SearchServiceOnboardingIndex>> requestCaptor =
        ArgumentCaptor.forClass(SearchServiceIndexRequest.class);
    verify(azureSearchRestClient).indexOnboarding(requestCaptor.capture());
    assertEquals("mergeOrUpload", requestCaptor.getValue().getValue().get(0).getAction());
  }

  @Test
  void testSearchOnboarding() {
    when(azureSearchRestClient.searchOnboarding("\\\"search\\\"", "all", "filter", true, 15L, 0L, null, "orderBy"))
            .thenReturn(new SearchServiceIndexResponse<>());
    searchServiceConnector.searchOnboarding("search", "filter", 15L, 0L, "orderBy");
    verify(azureSearchRestClient, times(1))
            .searchOnboarding("\\\"search\\\"", "all", "filter", true, 15L, 0L, null, "orderBy");
  }

  @Test
  void testOptimizeSearchString() {
    assertEquals("\\\"Test\\\" \\\"Test\\\" \\\"Test\\\"", searchServiceConnector.optimizeSearchString("Test* +Test -Test"));
    assertEquals("\\\"test\\\"", searchServiceConnector.optimizeSearchString("\"test\""));
    assertEquals("", searchServiceConnector.optimizeSearchString("    "));
    assertEquals("", searchServiceConnector.optimizeSearchString("  *  "));
    assertEquals("", searchServiceConnector.optimizeSearchString("di a"));
    assertEquals("\\\"test\\\"", searchServiceConnector.optimizeSearchString("di a test"));
    assertEquals("\\\"ACME\\\"", searchServiceConnector.optimizeSearchString("A.C.M.E"));
    assertEquals("\\\"Più\\\"", searchServiceConnector.optimizeSearchString("50&Più"));
    assertEquals("\\\"Aquila\\\"", searchServiceConnector.optimizeSearchString("L'Aquila"));
    assertNull(searchServiceConnector.optimizeSearchString(null));
  }

}
