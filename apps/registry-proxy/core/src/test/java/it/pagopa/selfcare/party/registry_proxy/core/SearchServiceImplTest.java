package it.pagopa.selfcare.party.registry_proxy.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.party.registry_proxy.connector.api.IpaSearchServiceConnector;
import it.pagopa.selfcare.party.registry_proxy.connector.api.SearchServiceConnector;
import it.pagopa.selfcare.party.registry_proxy.connector.exception.ServiceUnavailableException;
import it.pagopa.selfcare.party.registry_proxy.connector.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ContextConfiguration(classes = {SearchServiceImplTest.class})
@ExtendWith(MockitoExtension.class)
public class SearchServiceImplTest {

  @Mock
  private SearchServiceConnector searchServiceConnector;

  @Mock
  private IpaSearchServiceConnector ipaSearchServiceConnector;

  @InjectMocks
  private SearchServiceImpl searchService;

  @Autowired
  protected ObjectMapper mapper;

  @BeforeEach
  void setUp() {
    // Manually inject values for @Value fields since we are not using a full Spring Context
    ReflectionTestUtils.setField(searchService, "queueBindingName", "test-pubsub");
    ReflectionTestUtils.setField(searchService, "kafkaTopic", "test-topic");
  }

  @Test
  void subscribe_shouldReturnCorrectConfiguration() {
    // Act
    List<Map<String, String>> subscriptions = searchService.subscribe();

    // Assert
    assertNotNull(subscriptions);
    assertEquals(1, subscriptions.size());
    Map<String, String> subscription = subscriptions.get(0);
    assertEquals("test-pubsub", subscription.get("pubsubname"));
    assertEquals("test-topic", subscription.get("topic")); // Note: Corrected to use the injected value
    assertEquals("/dapr/events", subscription.get("route"));
  }

  @Test
  void searchInstitutionTest() {
    final String searchText = "Test";
    final long top = 5L;
    final OnboardingIndexSearch mockResponse = new OnboardingIndexSearch();
    final OnboardingIndex onboardingIndex1 = new OnboardingIndex();
    onboardingIndex1.setInstitutionId("inst-1");
    final OnboardingIndex onboardingIndex1Copy = new OnboardingIndex();
    onboardingIndex1Copy.setInstitutionId("inst-1");
    final OnboardingIndex onboardingIndex2 = new OnboardingIndex();
    onboardingIndex2.setInstitutionId("inst-2");
    mockResponse.setOnboardings(List.of(onboardingIndex1, onboardingIndex1Copy, onboardingIndex2));
    mockResponse.setTotalElements(3L);
    final OnboardingIndexSearch emptyResponse = new OnboardingIndexSearch();
    emptyResponse.setOnboardings(List.of());
    when(searchServiceConnector.searchOnboarding(any(), any(), any(), any(), any())).thenReturn(mockResponse).thenReturn(emptyResponse);
    final List<SearchServiceInstitution> institutions = searchService.searchInstitution(searchText, top);
    verify(searchServiceConnector, times(2)).searchOnboarding(any(), any(), any(), any(), any());
    assertEquals(2, institutions.size());
    assertEquals("inst-1", institutions.get(0).getId());
    assertEquals("inst-2", institutions.get(1).getId());
  }

  @Test
  void searchInstitutionTest_withLimit() {
    final String searchText = "Test";
    final long top = 1L;
    final OnboardingIndexSearch mockResponse = new OnboardingIndexSearch();
    final OnboardingIndex onboardingIndex1 = new OnboardingIndex();
    onboardingIndex1.setInstitutionId("inst-1");
    final OnboardingIndex onboardingIndex1Copy = new OnboardingIndex();
    onboardingIndex1Copy.setInstitutionId("inst-1");
    final OnboardingIndex onboardingIndex2 = new OnboardingIndex();
    onboardingIndex2.setInstitutionId("inst-2");
    mockResponse.setOnboardings(List.of(onboardingIndex1, onboardingIndex1Copy, onboardingIndex2));
    mockResponse.setTotalElements(3L);
    final OnboardingIndexSearch emptyResponse = new OnboardingIndexSearch();
    emptyResponse.setOnboardings(List.of());
    when(searchServiceConnector.searchOnboarding(any(), any(), any(), any(), any())).thenReturn(mockResponse).thenReturn(emptyResponse);
    final List<SearchServiceInstitution> institutions = searchService.searchInstitution(searchText, top);
    verify(searchServiceConnector, times(1)).searchOnboarding(any(), any(), any(), any(), any());
    assertEquals(1, institutions.size());
    assertEquals("inst-1", institutions.get(0).getId());
  }

  @Test
  void indexOnboardingTest() {
    final SearchServiceStatus status = new SearchServiceStatus();
    final AzureSearchValue value = new AzureSearchValue();
    value.setStatus(true);
    status.setValue(List.of(value));
    when(searchServiceConnector.indexOnboarding(any(OnboardingIndex.class))).thenReturn(status);
    assertDoesNotThrow(() -> searchService.indexOnboarding(new OnboardingIndex()));
    verify(searchServiceConnector, times(1)).indexOnboarding(any(OnboardingIndex.class));
  }

  @Test
  void indexOnboardingTest_serviceUnavailable() {
    final SearchServiceStatus status = new SearchServiceStatus();
    final AzureSearchValue value = new AzureSearchValue();
    value.setStatus(false);
    status.setValue(List.of(value));
    when(searchServiceConnector.indexOnboarding(any(OnboardingIndex.class))).thenReturn(status);
    assertThrows(ServiceUnavailableException.class, () -> searchService.indexOnboarding(new OnboardingIndex()));
    verify(searchServiceConnector, times(1)).indexOnboarding(any(OnboardingIndex.class));
  }

  @Test
  void searchOnboardingTest() {
    final String searchText = "Test";
    final List<String> products = List.of("prod-io", "prod-pagopa");
    final List<String> institutionTypes = List.of("PA", "GSP");
    final List<String> statuses = List.of("ACTIVE", "PENDING");
    final OffsetDateTime createdFromDate = OffsetDateTime.parse("2023-01-01T00:00:00Z");
    final OffsetDateTime createdToDate = OffsetDateTime.parse("2023-12-31T23:59:59Z");
    final List<String> orderBy = List.of("createdAt_ASC", "description_DESC");
    final Long page = 2L;
    final Long pageSize = 10L;
    final String orderByString = "createdAt asc,description desc";
    final String filter = "search.in(productId, 'prod-io,prod-pagopa') and search.in(institutionType, 'PA,GSP') and search.in(status, 'ACTIVE,PENDING') and createdAt ge 2023-01-01T00:00:00Z and createdAt le 2023-12-31T23:59:59Z";
    final OnboardingIndexSearch mockResponse = new OnboardingIndexSearch();
    mockResponse.setTotalElements(100L);
    when(searchServiceConnector.searchOnboarding(searchText, filter, pageSize, 20L, orderByString)).thenReturn(mockResponse);
    final OnboardingIndexSearch onboardingIndexSearch = searchService.searchOnboarding(searchText, products, institutionTypes, statuses, createdFromDate, createdToDate, page, pageSize, orderBy);
    verify(searchServiceConnector, times(1)).searchOnboarding(searchText, filter, pageSize, 20L, orderByString);
    assertEquals(page, onboardingIndexSearch.getPage());
    assertEquals(pageSize, onboardingIndexSearch.getPageSize());
    assertEquals(100L, mockResponse.getTotalElements());
    assertEquals(10L, onboardingIndexSearch.getTotalPages());
  }

  @Test
  void searchOnboardingTest_withoutStatusAndOptionalFields() {
    final String searchText = "Test";
    final List<String> products = List.of("prod-io", "prod-pagopa");
    final List<String> institutionTypes = List.of("PA", "GSP");
    final List<String> statuses = List.of();
    final String filter = "search.in(productId, 'prod-io,prod-pagopa') and search.in(institutionType, 'PA,GSP')";
    final OnboardingIndexSearch mockResponse = new OnboardingIndexSearch();
    mockResponse.setTotalElements(100L);
    when(searchServiceConnector.searchOnboarding(searchText, filter, 15L, 0L, "description asc")).thenReturn(mockResponse);
    final OnboardingIndexSearch onboardingIndexSearch = searchService.searchOnboarding(searchText, products, institutionTypes, statuses, null, null, null, null, null);
    verify(searchServiceConnector, times(1)).searchOnboarding(searchText, filter, 15L, 0L, "description asc");
    assertEquals(0L, onboardingIndexSearch.getPage());
    assertEquals(15L, onboardingIndexSearch.getPageSize());
    assertEquals(100L, mockResponse.getTotalElements());
    assertEquals(7L, onboardingIndexSearch.getTotalPages());
  }

  @Test
  void searchOnboardingTest_withoutStatusAndInstitutionTypes() {
    final String searchText = "Test";
    final List<String> products = List.of("prod-io", "prod-pagopa");
    final List<String> institutionTypes = null;
    final List<String> statuses = List.of();
    final String filter = "search.in(productId, 'prod-io,prod-pagopa')";
    final OnboardingIndexSearch mockResponse = new OnboardingIndexSearch();
    mockResponse.setTotalElements(100L);
    when(searchServiceConnector.searchOnboarding(searchText, filter, 15L, 0L, "description asc")).thenReturn(mockResponse);
    final OnboardingIndexSearch onboardingIndexSearch = searchService.searchOnboarding(searchText, products, institutionTypes, statuses, null, null, null, null, null);
    verify(searchServiceConnector, times(1)).searchOnboarding(searchText, filter, 15L, 0L, "description asc");
    assertEquals(0L, onboardingIndexSearch.getPage());
    assertEquals(15L, onboardingIndexSearch.getPageSize());
    assertEquals(100L, mockResponse.getTotalElements());
    assertEquals(7L, onboardingIndexSearch.getTotalPages());
  }

  @Test
  void searchOnboardingTest_withoutStatusInstitutionTypeAndProducts() {
    final String searchText = "Test";
    final List<String> products = List.of();
    final List<String> institutionTypes = null;
    final List<String> statuses = List.of();
    final String filter = "";
    final OnboardingIndexSearch mockResponse = new OnboardingIndexSearch();
    mockResponse.setTotalElements(100L);
    when(searchServiceConnector.searchOnboarding(searchText, filter, 15L, 0L, "description asc")).thenReturn(mockResponse);
    final OnboardingIndexSearch onboardingIndexSearch = searchService.searchOnboarding(searchText, products, institutionTypes, statuses, null, null, null, null, null);
    verify(searchServiceConnector, times(1)).searchOnboarding(searchText, filter, 15L, 0L, "description asc");
    assertEquals(0L, onboardingIndexSearch.getPage());
    assertEquals(15L, onboardingIndexSearch.getPageSize());
    assertEquals(100L, mockResponse.getTotalElements());
    assertEquals(7L, onboardingIndexSearch.getTotalPages());
  }

  @Test
  void searchOnboardingWhenOrderIsNotValid() {
    final String searchText = "Test";
    final List<String> products = List.of("prod-io", "prod-pagopa");
    final List<String> institutionTypes = List.of("PA", "GSP");
    final List<String> statuses = List.of("ACTIVE", "PENDING");
    final List<String> orderBy = List.of("createdAt_AS", "description_DESC");
    final Long page = 2L;
    final Long pageSize = 10L;
    final OnboardingIndexSearch mockResponse = new OnboardingIndexSearch();
    mockResponse.setTotalElements(100L);
    assertThrows(IllegalArgumentException.class, () -> searchService.searchOnboarding(searchText, products, institutionTypes, statuses, null, null, page, pageSize, orderBy));
  }

  @Test
  void searchOnboardingWhenOrderIsNotValid2() {
    final String searchText = "Test";
    final List<String> products = List.of("prod-io", "prod-pagopa");
    final List<String> institutionTypes = List.of("PA", "GSP");
    final List<String> statuses = List.of("ACTIVE", "PENDING");
    final List<String> orderBy = List.of("createdAt_ASC_DESC", "description_DESC");
    final Long page = 2L;
    final Long pageSize = 10L;
    final OnboardingIndexSearch mockResponse = new OnboardingIndexSearch();
    mockResponse.setTotalElements(100L);
    assertThrows(IllegalArgumentException.class, () -> searchService.searchOnboarding(searchText, products, institutionTypes, statuses, null, null, page, pageSize, orderBy));
  }

  @Test
  void searchIpaInstitutions_shouldCallConnector() {
    IpaInstitutionSearchResult mockResult = new IpaInstitutionSearchResult();
    IpaInstitution institution = new IpaInstitution();
    institution.setId("ipa-1");
    institution.setDescription("Comune di Roma");
    mockResult.setInstitutions(List.of(institution));
    mockResult.setTotalElements(1L);

    when(ipaSearchServiceConnector.search("Roma", null, 50, 0))
            .thenReturn(mockResult);

    IpaInstitutionSearchResult result = searchService.searchIpaInstitutions("Roma", null, 0, 50);

    assertNotNull(result);
    assertEquals(1L, result.getTotalElements());
    assertEquals("ipa-1", result.getInstitutions().get(0).getId());
    verify(ipaSearchServiceConnector, times(1)).search("Roma", null, 50, 0);
  }

  @Test
  void searchIpaInstitutions_withCategory_shouldBuildFilter() {
    IpaInstitutionSearchResult mockResult = new IpaInstitutionSearchResult();
    mockResult.setInstitutions(List.of());
    mockResult.setTotalElements(0L);

    when(ipaSearchServiceConnector.search("*", "category eq 'L6'", 50, 0))
            .thenReturn(mockResult);

    IpaInstitutionSearchResult result = searchService.searchIpaInstitutions(null, "L6", 0, 50);

    assertNotNull(result);
    assertEquals(0L, result.getTotalElements());
    verify(ipaSearchServiceConnector, times(1)).search("*", "category eq 'L6'", 50, 0);
  }

  @Test
  void searchIpaInstitutions_withDefaultPagination() {
    IpaInstitutionSearchResult mockResult = new IpaInstitutionSearchResult();
    mockResult.setInstitutions(List.of());
    mockResult.setTotalElements(0L);

    when(ipaSearchServiceConnector.search("*", null, 50, 0))
            .thenReturn(mockResult);

    IpaInstitutionSearchResult result = searchService.searchIpaInstitutions(null, null, null, null);

    assertNotNull(result);
    verify(ipaSearchServiceConnector, times(1)).search("*", null, 50, 0);
  }

}
