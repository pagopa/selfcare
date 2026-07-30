package it.pagopa.selfcare.party.registry_proxy.connector.rest;

import it.pagopa.selfcare.party.registry_proxy.connector.model.OnboardingIndex;
import it.pagopa.selfcare.party.registry_proxy.connector.model.SearchServiceStatus;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.client.AzureSearchRestClient;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.mapper.SearchServiceMapperImpl;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.search.SearchServiceIndexResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
  void testIndexOnboarding() {
    when(azureSearchRestClient.indexOnboarding(any())).thenReturn(new SearchServiceStatus());
    searchServiceConnector.indexOnboarding(new OnboardingIndex());
    verify(azureSearchRestClient, times(1)).indexOnboarding(any());
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

