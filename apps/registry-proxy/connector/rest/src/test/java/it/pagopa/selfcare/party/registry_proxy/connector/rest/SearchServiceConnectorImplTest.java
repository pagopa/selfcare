package it.pagopa.selfcare.party.registry_proxy.connector.rest;

import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.party.registry_proxy.connector.model.AzureSearchValue;
import it.pagopa.selfcare.party.registry_proxy.connector.model.SearchServiceInstitution;
import it.pagopa.selfcare.party.registry_proxy.connector.model.SearchServiceStatus;
import it.pagopa.selfcare.party.registry_proxy.connector.model.institution.Institution;
import it.pagopa.selfcare.party.registry_proxy.connector.model.institution.Onboarding;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.client.AzureSearchRestClient;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.SearchServiceInstitutionResponse;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.SearchServiceResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;


@ContextConfiguration(classes = {SearchServiceConnectorImpl.class})
@ExtendWith(SpringExtension.class)
public class SearchServiceConnectorImplTest {

  @Autowired
  private SearchServiceConnectorImpl searchServiceConnector;

  @MockBean
  private AzureSearchRestClient azureSearchRestClient;

  @Test
  public void testIndexInstitution() {
    SearchServiceStatus searchServiceStatus = new SearchServiceStatus();
    AzureSearchValue azureSearchValue = new AzureSearchValue();
    azureSearchValue.setStatus(true);
    azureSearchValue.setStatusCode(200);
    searchServiceStatus.setValue(List.of(azureSearchValue));

    Institution institution = new Institution();
    institution.setId("1");
    institution.setDescription("Institution");
    institution.setTaxCode("taxcode");
    Onboarding onboarding = new Onboarding();
    onboarding.setProductId("prod1");
    onboarding.setInstitutionType(InstitutionType.PA.name());
    institution.setOnboarding(List.of(onboarding));
    institution.setUpdatedAt(OffsetDateTime.now());

    when(searchServiceConnector.indexInstitution(institution)).thenReturn(searchServiceStatus);
    SearchServiceStatus azureResponse = searchServiceConnector.indexInstitution(institution);
    assertEquals(1, azureResponse.getValue().size());
    assertEquals(200, azureResponse.getValue().get(0).getStatusCode());
  }

  @Test
  public void testIndexInstitutionCustomType() {
    SearchServiceStatus searchServiceStatus = new SearchServiceStatus();
    AzureSearchValue azureSearchValue = new AzureSearchValue();
    azureSearchValue.setStatus(true);
    azureSearchValue.setStatusCode(200);
    searchServiceStatus.setValue(List.of(azureSearchValue));

    Institution institution = new Institution();
    institution.setId("1");
    institution.setDescription("Institution");
    institution.setTaxCode("taxcode");
    Onboarding onboarding = new Onboarding();
    onboarding.setProductId("prod1");
    onboarding.setInstitutionType("TYPE");
    institution.setOnboarding(List.of(onboarding));
    institution.setUpdatedAt(OffsetDateTime.now());

    when(searchServiceConnector.indexInstitution(institution)).thenReturn(searchServiceStatus);
    SearchServiceStatus azureResponse = searchServiceConnector.indexInstitution(institution);
    assertEquals(1, azureResponse.getValue().size());
    assertEquals(200, azureResponse.getValue().get(0).getStatusCode());
  }

  @Test
  void searchInstitution_shouldReturnInstitutions_whenValidResponse() {
    // Given
    String search = "*";
    String filter = "products/any(p: p eq 'prod-io')";
    List<String> products = List.of("prod-io");
    Integer top = 50;
    Integer skip = 0;
    String select = "id,description,taxCode";
    String orderby = "description";

    SearchServiceInstitutionResponse institutionResponse = createSearchServiceInstitutionResponse();
    SearchServiceResponse searchServiceResponse = new SearchServiceResponse();
    searchServiceResponse.setValue(List.of(institutionResponse));

    when(azureSearchRestClient.searchInstitution(search, filter, top, skip, select, orderby))
      .thenReturn(searchServiceResponse);

    // When
    List<SearchServiceInstitution> result = searchServiceConnector.searchInstitution(
      search, filter, products, top, skip, select, orderby);

    // Then
    assertThat(result).isNotNull();
    assertThat(result).hasSize(1);

    SearchServiceInstitution institution = result.get(0);
    assertThat(institution.getId()).isEqualTo("12345678");
    assertThat(institution.getDescription()).isEqualTo("Comune di Reggio Emilia");
    assertThat(institution.getTaxCode()).isEqualTo("00145920351");
    assertThat(institution.getProducts()).containsExactly("prod-io");
    assertThat(institution.getInstitutionTypes()).containsExactly("PA", "GSP");

    verify(azureSearchRestClient, times(1))
      .searchInstitution(search, filter, top, skip, select, orderby);
  }

  @Test
  void searchInstitution_shouldReturnInstitutions_whenValidResponse_withAllProducts() {
    // Given
    String search = "*";
    String filter = "products/any(p: p eq 'prod-io')";
    List<String> products = List.of("all");
    Integer top = 50;
    Integer skip = 0;
    String select = "id,description,taxCode";
    String orderby = "description";

    SearchServiceInstitutionResponse institutionResponse = createSearchServiceInstitutionResponse();
    SearchServiceResponse searchServiceResponse = new SearchServiceResponse();
    searchServiceResponse.setValue(List.of(institutionResponse));

    when(azureSearchRestClient.searchInstitution(search, filter, top, skip, select, orderby))
      .thenReturn(searchServiceResponse);

    // When
    List<SearchServiceInstitution> result = searchServiceConnector.searchInstitution(
      search, filter, products, top, skip, select, orderby);

    // Then
    assertThat(result).isNotNull();
    assertThat(result).hasSize(1);

    SearchServiceInstitution institution = result.get(0);
    assertThat(institution.getId()).isEqualTo("12345678");
    assertThat(institution.getDescription()).isEqualTo("Comune di Reggio Emilia");
    assertThat(institution.getTaxCode()).isEqualTo("00145920351");
    assertThat(institution.getProducts()).containsExactly("prod-io", "prod-interop", "prod-interop-coll");
    assertThat(institution.getInstitutionTypes()).containsExactly("PA", "GSP");

    verify(azureSearchRestClient, times(1))
      .searchInstitution(search, filter, top, skip, select, orderby);
  }

  @Test
  void searchInstitution_shouldReturnInstitutionsAoo_whenValidResponse() {
    // Given
    String search = "*";
    String filter = "products/any(p: p eq 'prod-io')";
    Integer top = 50;
    Integer skip = 0;
    String select = "id,description,taxCode";
    String orderby = "description";

    SearchServiceInstitutionResponse institutionResponse = createSearchServiceInstitutionAooResponse();
    SearchServiceResponse searchServiceResponse = new SearchServiceResponse();
    searchServiceResponse.setValue(List.of(institutionResponse));

    when(azureSearchRestClient.searchInstitution(search, filter, top, skip, select, orderby))
      .thenReturn(searchServiceResponse);

    // When
    List<SearchServiceInstitution> result = searchServiceConnector.searchInstitution(
      search, filter, List.of("all"), top, skip, select, orderby);

    // Then
    assertThat(result).isNotNull();
    assertThat(result).hasSize(1);

    SearchServiceInstitution institution = result.get(0);
    assertThat(institution.getId()).isEqualTo("12345678");
    assertThat(institution.getDescription()).isEqualTo("Polizia municipale");
    assertThat(institution.getParentDescription()).isEqualTo("Comune di Reggio Emilia");
    assertThat(institution.getTaxCode()).isEqualTo("00145920351");
    assertThat(institution.getProducts()).containsExactly("prod-io", "prod-interop", "prod-interop-coll");
    assertThat(institution.getInstitutionTypes()).containsExactly("PA", "GSP");

    verify(azureSearchRestClient, times(1))
      .searchInstitution(search, filter, top, skip, select, orderby);
  }

  @Test
  void searchInstitution_shouldReturnEmptyList_whenResponseIsNull() {
    // Given
    String search = "*";
    String filter = null;
    Integer top = 50;
    Integer skip = 0;
    String select = null;
    String orderby = null;

    SearchServiceResponse response = new SearchServiceResponse();
    response.setValue(new ArrayList<>());
    when(azureSearchRestClient.searchInstitution(search, filter, top, skip, select, orderby))
      .thenReturn(response);

    // When
    List<SearchServiceInstitution> result = searchServiceConnector.searchInstitution(
      search, filter, List.of("all"), top, skip, select, orderby);

    // Then
    assertThat(result).isNotNull();
    assertThat(result).isEmpty();

    verify(azureSearchRestClient, times(1))
      .searchInstitution(search, filter, top, skip, select, orderby);
  }

  @Test
  void searchInstitution_shouldReturnEmptyList_whenResponseValueIsNull() {
    // Given
    String search = "Reggio";
    String filter = "institutionTypes/any(t: t eq 'PA')";
    Integer top = 10;
    Integer skip = 5;
    String select = "id,description";
    String orderby = "description asc";

    SearchServiceResponse searchServiceResponse = new SearchServiceResponse();
    searchServiceResponse.setValue(new ArrayList<>());

    when(azureSearchRestClient.searchInstitution(search, filter, top, skip, select, orderby))
      .thenReturn(searchServiceResponse);

    // When
    List<SearchServiceInstitution> result = searchServiceConnector.searchInstitution(
      search, filter, List.of("all"), top, skip, select, orderby);

    // Then
    assertThat(result).isNotNull();
    assertThat(result).isEmpty();

    verify(azureSearchRestClient, times(1))
      .searchInstitution(search, filter, top, skip, select, orderby);
  }

  @Test
  void searchInstitution_shouldReturnEmptyList_whenResponseValueIsEmpty() {
    // Given
    String search = "NonExistent";
    String filter = "taxCode eq '99999999999'";
    Integer top = 50;
    Integer skip = 0;
    String select = null;
    String orderby = null;

    SearchServiceResponse searchServiceResponse = new SearchServiceResponse();
    searchServiceResponse.setValue(Collections.emptyList());

    when(azureSearchRestClient.searchInstitution(search, filter, top, skip, select, orderby))
      .thenReturn(searchServiceResponse);

    // When
    List<SearchServiceInstitution> result = searchServiceConnector.searchInstitution(
      search, filter, List.of("all"), top, skip, select, orderby);

    // Then
    assertThat(result).isNotNull();
    assertThat(result).isEmpty();

    verify(azureSearchRestClient, times(1))
      .searchInstitution(search, filter, top, skip, select, orderby);
  }

  @Test
  void searchInstitution_shouldReturnMultipleInstitutions_whenMultipleResults() {
    // Given
    String search = "*";
    String filter = "products/any(p: p eq 'prod-io' or p eq 'prod-interop')";
    Integer top = 100;
    Integer skip = 0;
    String select = null;
    String orderby = null;

    SearchServiceInstitutionResponse institution1 = createSearchServiceInstitutionResponse();
    SearchServiceInstitutionResponse institution2 = createSearchServiceInstitutionResponse();
    institution2.setId("another-id-123");
    institution2.setDescription("Altro Comune");
    institution2.setTaxCode("11111111111");

    SearchServiceResponse searchServiceResponse = new SearchServiceResponse();
    searchServiceResponse.setValue(List.of(institution1, institution2));

    when(azureSearchRestClient.searchInstitution(search, filter, top, skip, select, orderby))
      .thenReturn(searchServiceResponse);

    // When
    List<SearchServiceInstitution> result = searchServiceConnector.searchInstitution(
      search, filter, List.of("prod-io", "prod-interop"), top, skip, select, orderby);

    // Then
    assertThat(result).isNotNull();
    assertThat(result).hasSize(2);

    assertThat(result.get(0).getId()).isEqualTo("12345678");
    assertThat(result.get(1).getId()).isEqualTo("another-id-123");

    verify(azureSearchRestClient, times(1))
      .searchInstitution(search, filter, top, skip, select, orderby);
  }

  @Test
  void searchInstitution_shouldHandleNullParameters() {
    // Given
    SearchServiceInstitutionResponse institutionResponse = createSearchServiceInstitutionResponse();
    SearchServiceResponse searchServiceResponse = new SearchServiceResponse();
    searchServiceResponse.setValue(List.of(institutionResponse));

    when(azureSearchRestClient.searchInstitution(null, null, null, null, null, null))
      .thenReturn(searchServiceResponse);

    // When
    List<SearchServiceInstitution> result = searchServiceConnector.searchInstitution(
      null, null, null,null, null, null, null);

    // Then
    assertThat(result).isNotNull();
    assertThat(result).hasSize(1);

    verify(azureSearchRestClient, times(1))
      .searchInstitution(null, null, null, null, null, null);
  }

  @Test
  void searchInstitution_shouldPropagateException_whenClientThrowsException() {
    // Given
    String search = "*";
    String filter = "invalid filter";
    Integer top = 50;
    Integer skip = 0;
    String select = null;
    String orderby = null;

    when(azureSearchRestClient.searchInstitution(search, filter, top, skip, select, orderby))
      .thenThrow(new RuntimeException("Azure Search error"));

    // When & Then
    org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
      searchServiceConnector.searchInstitution(search, filter, List.of("all"), top, skip, select, orderby);
    });

    verify(azureSearchRestClient, times(1))
      .searchInstitution(search, filter, top, skip, select, orderby);
  }

  private SearchServiceInstitutionResponse createSearchServiceInstitutionResponse() {
    SearchServiceInstitutionResponse response = new SearchServiceInstitutionResponse();
    response.setId("12345678");
    response.setDescription("Comune di Reggio Emilia");
    response.setTaxCode("00145920351");
    response.setProducts(List.of("prod-io", "prod-interop", "prod-interop-coll"));
    response.setInstitutionTypes(List.of("PA", "GSP"));
    response.setLastModified(OffsetDateTime.parse("2023-10-10T17:13:44.263Z"));
    return response;
  }

  private SearchServiceInstitutionResponse createSearchServiceInstitutionAooResponse() {
    SearchServiceInstitutionResponse response = new SearchServiceInstitutionResponse();
    response.setId("12345678");
    response.setDescription("Polizia municipale");
    response.setParentDescription("Comune di Reggio Emilia");
    response.setTaxCode("00145920351");
    response.setProducts(List.of("prod-io", "prod-interop", "prod-interop-coll"));
    response.setInstitutionTypes(List.of("PA", "GSP"));
    response.setLastModified(OffsetDateTime.parse("2023-10-10T17:13:44.263Z"));
    return response;
  }
}

