package it.pagopa.selfcare.party.registry_proxy.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.party.registry_proxy.connector.api.InstitutionConnector;
import it.pagopa.selfcare.party.registry_proxy.connector.api.SearchServiceConnector;
import it.pagopa.selfcare.party.registry_proxy.connector.exception.ResourceNotFoundException;
import it.pagopa.selfcare.party.registry_proxy.connector.exception.ServiceUnavailableException;
import it.pagopa.selfcare.party.registry_proxy.connector.model.*;
import it.pagopa.selfcare.party.registry_proxy.connector.model.institution.Institution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ContextConfiguration(classes = {SearchServiceImplTest.class})
@ExtendWith(MockitoExtension.class)
public class SearchServiceImplTest {

  @Mock
  private InstitutionConnector institutionConnector;

  @Mock
  private SearchServiceConnector searchServiceConnector;

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
  void indexInstitution_shouldReturnTrue_whenIndexingIsSuccessful() {
    // Arrange
    String institutionId = "test-id";
    Institution mockInstitution = new Institution(); // Assume a valid institution object
    AzureSearchValue mockSearchValue = new AzureSearchValue();
    mockSearchValue.setStatus(true);
    SearchServiceStatus mockSearchStatus = new SearchServiceStatus();
    mockSearchStatus.setValue(Collections.singletonList(mockSearchValue));

    when(institutionConnector.getById(institutionId)).thenReturn(mockInstitution);
    when(searchServiceConnector.indexInstitution(mockInstitution)).thenReturn(mockSearchStatus);

    // Act
    boolean result = searchService.indexInstitution(institutionId);

    // Assert
    assertTrue(result);
    verify(institutionConnector, times(1)).getById(institutionId);
    verify(searchServiceConnector, times(1)).indexInstitution(mockInstitution);
  }

  @Test
  void indexInstitution_shouldThrowResourceNotFoundException_whenInstitutionIsNull() {
    // Arrange
    String institutionId = "not-found-id";
    when(institutionConnector.getById(institutionId)).thenReturn(null);

    // Act & Assert
    assertThrows(ResourceNotFoundException.class, () -> searchService.indexInstitution(institutionId));
    verify(institutionConnector, times(1)).getById(institutionId);
    verify(searchServiceConnector, never()).indexInstitution(any());
  }

  @Test
  void indexInstitution_shouldThrowServiceUnavailableException_whenSearchStatusIsNull() {
    // Arrange
    String institutionId = "test-id";
    Institution mockInstitution = new Institution();
    when(institutionConnector.getById(institutionId)).thenReturn(mockInstitution);
    when(searchServiceConnector.indexInstitution(mockInstitution)).thenReturn(null);

    // Act & Assert
    assertThrows(ServiceUnavailableException.class, () -> searchService.indexInstitution(institutionId));
  }

  @Test
  void indexInstitution_shouldThrowServiceUnavailableException_whenSearchStatusValueIsEmpty() {
    // Arrange
    String institutionId = "test-id";
    Institution mockInstitution = new Institution();
    SearchServiceStatus emptyStatus = new SearchServiceStatus();
    emptyStatus.setValue(Collections.emptyList());
    when(institutionConnector.getById(institutionId)).thenReturn(mockInstitution);
    when(searchServiceConnector.indexInstitution(mockInstitution)).thenReturn(emptyStatus);

    // Act & Assert
    assertThrows(ServiceUnavailableException.class, () -> searchService.indexInstitution(institutionId));
  }

  @Test
  void indexInstitution_shouldThrowServiceUnavailableException_whenIndexingFails() {
    // Arrange
    String institutionId = "test-id";
    Institution mockInstitution = new Institution();
    AzureSearchValue failedValue = new AzureSearchValue();
    failedValue.setStatus(false);
    SearchServiceStatus failedStatus = new SearchServiceStatus();
    failedStatus.setValue(Collections.singletonList(failedValue));
    when(institutionConnector.getById(institutionId)).thenReturn(mockInstitution);
    when(searchServiceConnector.indexInstitution(mockInstitution)).thenReturn(failedStatus);

    // Act & Assert
    assertThrows(ServiceUnavailableException.class, () -> searchService.indexInstitution(institutionId));
  }

  @Test
  void buildFilter_withAllParameters() {
    // Arrange
    List<String> products = List.of("prod-io", "prod-pagopa");
    List<String> institutionTypes = List.of("PA", "GSP");
    String taxCode = "12345678901";
    String expectedFilter = "products/any(p: p eq 'prod-io' or p eq 'prod-pagopa') and institutionTypes/any(t: t eq 'PA' or t eq 'GSP') and taxCode eq '12345678901'";

    // Act
    String result = searchService.buildFilter(products, institutionTypes, taxCode);

    // Assert
    assertEquals(expectedFilter, result);
  }

  @Test
  void buildFilter_withAllParameters_andProductAll() {
    // Arrange
    List<String> products = List.of("all");
    List<String> institutionTypes = List.of("PA", "GSP");
    String taxCode = "12345678901";
    String expectedFilter = "institutionTypes/any(t: t eq 'PA' or t eq 'GSP') and taxCode eq '12345678901'";

    // Act
    String result = searchService.buildFilter(products, institutionTypes, taxCode);

    // Assert
    assertEquals(expectedFilter, result);
  }

  @Test
  void buildFilter_withOnlyProducts() {
    // Arrange
    List<String> products = List.of("prod-io");
    String expectedFilter = "products/any(p: p eq 'prod-io')";

    // Act
    String result = searchService.buildFilter(products, null, null);

    // Assert
    assertEquals(expectedFilter, result);
  }

  @Test
  void buildFilter_withOnlyProductsAndAll() {
    // Arrange
    List<String> products = List.of("prod-io", "all");
    String expectedFilter = "";

    // Act
    String result = searchService.buildFilter(products, null, null);

    // Assert
    assertEquals(expectedFilter, result);
  }

  @Test
  void buildFilter_withOnlyInstitutionTypes() {
    // Arrange
    List<String> institutionTypes = List.of("PA");
    String expectedFilter = "institutionTypes/any(t: t eq 'PA')";

    // Act
    String result = searchService.buildFilter(null, institutionTypes, null);

    // Assert
    assertEquals(expectedFilter, result);
  }

  @Test
  void buildFilter_withOnlyTaxCode() {
    // Arrange
    String taxCode = "12345678901";
    String expectedFilter = "taxCode eq '12345678901'";

    // Act
    String result = searchService.buildFilter(null, null, taxCode);

    // Assert
    assertEquals(expectedFilter, result);
  }

  @Test
  void buildFilter_withNullAndEmptyParameters() {
    // Arrange & Act
    String result = searchService.buildFilter(null, Collections.emptyList(), "");

    // Assert
    assertEquals("", result);
  }


  // --- Test for searchInstitution ---

  @Test
  void searchInstitution_shouldCallConnectorWithCorrectFilter() {
    // Arrange
    String search = "Comune";
    List<String> products = List.of("prod-io");
    List<String> institutionTypes = List.of("PA");
    String taxCode = "12345678901";
    Integer top = 10;
    Integer skip = 0;

    String expectedFilter = "products/any(p: p eq 'prod-io') and institutionTypes/any(t: t eq 'PA') and taxCode eq '12345678901'";
    List<SearchServiceInstitution> mockResponse = Collections.singletonList(new SearchServiceInstitution());

    when(searchServiceConnector.searchInstitution(search, expectedFilter, products, top, skip, null, null))
      .thenReturn(mockResponse);

    // Act
    List<SearchServiceInstitution> result = searchService.searchInstitution(search, products, institutionTypes, taxCode, top, skip, null, null);

    // Assert
    assertNotNull(result);
    assertEquals(mockResponse, result);

    verify(searchServiceConnector, times(1)).searchInstitution(search, expectedFilter, products, top, skip, null, null);
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
    final Long page = 2L;
    final Long pageSize = 10L;
    final String orderBy = "createdAt asc";
    final String filter = "search.in(productId, 'prod-io,prod-pagopa') and search.in(institutionType, 'PA,GSP') and search.in(status, 'ACTIVE,PENDING')";
    final OnboardingIndexSearch mockResponse = new OnboardingIndexSearch();
    mockResponse.setTotalElements(100L);
    when(searchServiceConnector.searchOnboarding(searchText, filter, pageSize, 20L, orderBy)).thenReturn(mockResponse);
    final OnboardingIndexSearch onboardingIndexSearch = searchService.searchOnboarding(searchText, products, institutionTypes, statuses, page, pageSize, orderBy);
    verify(searchServiceConnector, times(1)).searchOnboarding(searchText, filter, pageSize, 20L, orderBy);
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
    final OnboardingIndexSearch onboardingIndexSearch = searchService.searchOnboarding(searchText, products, institutionTypes, statuses, null, null, null);
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
    final OnboardingIndexSearch onboardingIndexSearch = searchService.searchOnboarding(searchText, products, institutionTypes, statuses, null, null, null);
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
    final OnboardingIndexSearch onboardingIndexSearch = searchService.searchOnboarding(searchText, products, institutionTypes, statuses, null, null, null);
    verify(searchServiceConnector, times(1)).searchOnboarding(searchText, filter, 15L, 0L, "description asc");
    assertEquals(0L, onboardingIndexSearch.getPage());
    assertEquals(15L, onboardingIndexSearch.getPageSize());
    assertEquals(100L, mockResponse.getTotalElements());
    assertEquals(7L, onboardingIndexSearch.getTotalPages());
  }

}
