package it.pagopa.selfcare.party.registry_proxy.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.pagopa.selfcare.party.registry_proxy.connector.api.InstitutionConnector;
import it.pagopa.selfcare.party.registry_proxy.connector.api.SearchServiceConnector;
import it.pagopa.selfcare.party.registry_proxy.connector.exception.ResourceNotFoundException;
import it.pagopa.selfcare.party.registry_proxy.connector.exception.ServiceUnavailableException;
import it.pagopa.selfcare.party.registry_proxy.connector.model.AzureSearchValue;
import it.pagopa.selfcare.party.registry_proxy.connector.model.SearchServiceInstitution;
import it.pagopa.selfcare.party.registry_proxy.connector.model.SearchServiceStatus;
import it.pagopa.selfcare.party.registry_proxy.connector.model.institution.Institution;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class SearchServiceImpl implements SearchService {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final InstitutionConnector institutionConnector;
  private final SearchServiceConnector searchServiceConnector;

  @Value("${dapr.queue.binding-name}")
  private String queueBindingName;

  @Value("${dapr.queue.topic}")
  private String kafkaTopic;

  @Autowired
  public SearchServiceImpl(InstitutionConnector institutionConnector, SearchServiceConnector searchServiceConnector) {
    this.institutionConnector = institutionConnector;
    this.searchServiceConnector = searchServiceConnector;
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  }

  @Override
  public List<Map<String, String>> subscribe() {
    List<Map<String, String>> subscriptions = new ArrayList<>();
    Map<String, String> subscription = new HashMap<>();
    subscription.put("pubsubname", queueBindingName);
    subscription.put("topic", kafkaTopic);
    subscription.put("route", "/dapr/events");
    subscriptions.add(subscription);
    log.info("Dapr subscriptions configured: {}", subscriptions);
    return subscriptions;
  }

  @Override
  public boolean indexInstitution(String institutionId) {
    Institution institution = institutionConnector.getById(institutionId);
    if (institution == null) {
      throw new ResourceNotFoundException();
    }
    SearchServiceStatus status = searchServiceConnector.indexInstitution(institution);

    if (status == null || status.getValue() == null || status.getValue().isEmpty()) {
      throw new ServiceUnavailableException();
    }

    for (AzureSearchValue value : status.getValue()) {
      log.debug("Indexing status for institution {}: {}", institutionId, value.getStatus());
      if (!value.getStatus()) {
        throw new ServiceUnavailableException();
      }
    }
    return true;
  }

  @Override
  public List<SearchServiceInstitution> searchInstitution(String search, List<String> products, List<String> institutionTypes, String taxCode,
                                                          Integer top, Integer skip, String select, String orderby) {
    return searchServiceConnector.searchInstitution(search, buildFilter(products, institutionTypes, taxCode), products, top, skip, select, orderby);
  }

  public String buildFilter(List<String> products, List<String> institutionTypes, String taxCode) {
    StringBuilder filterBuilder = new StringBuilder();

    if (products != null && !products.isEmpty() && !products.contains("all")) {
      filterBuilder.append("products/any(p: ");
      for (int i = 0; i < products.size(); i++) {
        if (i > 0) {
          filterBuilder.append(" or ");
        }
        filterBuilder.append("p eq '").append(products.get(i)).append("'");
      }
      filterBuilder.append(")");
    }

    if (institutionTypes != null && !institutionTypes.isEmpty()) {
      if (!filterBuilder.isEmpty()) {
        filterBuilder.append(" and ");
      }
      filterBuilder.append("institutionTypes/any(t: ");
      for (int i = 0; i < institutionTypes.size(); i++) {
        if (i > 0) {
          filterBuilder.append(" or ");
        }
        filterBuilder.append("t eq '").append(institutionTypes.get(i)).append("'");
      }
      filterBuilder.append(")");
    }

    if (taxCode != null && !taxCode.isEmpty()) {
      if (!filterBuilder.isEmpty()) {
        filterBuilder.append(" and ");
      }
      filterBuilder.append("taxCode eq '").append(taxCode).append("'");
    }

    return filterBuilder.toString();
  }
}

