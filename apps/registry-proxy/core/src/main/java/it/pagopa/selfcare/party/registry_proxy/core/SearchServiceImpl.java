package it.pagopa.selfcare.party.registry_proxy.core;

import it.pagopa.selfcare.party.registry_proxy.connector.api.IpaSearchServiceConnector;
import it.pagopa.selfcare.party.registry_proxy.connector.api.SearchServiceConnector;
import it.pagopa.selfcare.party.registry_proxy.connector.exception.ServiceUnavailableException;
import it.pagopa.selfcare.party.registry_proxy.connector.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class SearchServiceImpl implements SearchService {

  public static final String AND = " and ";
  private final SearchServiceConnector searchServiceConnector;
  private final IpaSearchServiceConnector ipaSearchServiceConnector;

  @Value("${dapr.queue.binding-name}")
  private String queueBindingName;

  @Value("${dapr.queue.topic}")
  private String kafkaTopic;

  @Autowired
  public SearchServiceImpl(SearchServiceConnector searchServiceConnector,
                           IpaSearchServiceConnector ipaSearchServiceConnector) {
    this.searchServiceConnector = searchServiceConnector;
    this.ipaSearchServiceConnector = ipaSearchServiceConnector;
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
  public List<SearchServiceInstitution> searchInstitution(String search, Long top) {
    final long limit = top != null && top > 0L && top < Long.MAX_VALUE ? top : 50L;
    final Set<String> institutionIds = new HashSet<>();
    return Stream.iterate(0L, p -> p + 1)
        .map(p -> searchOnboarding(search, null, null, List.of("COMPLETED", "DELETED"), null, null, p, limit * 2L, List.of("description_ASC"), true))
        .takeWhile(result -> !result.getOnboardings().isEmpty())
        .flatMap(result -> result.getOnboardings().stream())
        .filter(onboarding -> Optional.ofNullable(onboarding.getInstitutionId()).map(institutionIds::add).orElse(false))
        .limit(limit)
        .map(o -> {
          final SearchServiceInstitution inst = new SearchServiceInstitution();
          inst.setId(o.getInstitutionId());
          inst.setDescription(o.getDescription());
          inst.setParentDescription(o.getParentDescription());
          inst.setTaxCode(o.getTaxCode());
          inst.setLastModified(o.getUpdatedAt());
          return inst;
        })
        .toList();
  }

  @Override
  public boolean indexOnboarding(OnboardingIndex onboardingIndex) {
    final SearchServiceStatus status = searchServiceConnector.indexOnboarding(onboardingIndex);

    if (status == null || status.getValue() == null || status.getValue().isEmpty()) {
      throw new ServiceUnavailableException();
    }

    for (AzureSearchValue value : status.getValue()) {
      log.debug("Indexing status for onboarding {}: {}", onboardingIndex.getOnboardingId(), value.getStatus());
      if (Boolean.FALSE.equals(value.getStatus())) {
        throw new ServiceUnavailableException();
      }
    }

    return true;
  }

  @Override
  public OnboardingIndexSearch searchOnboarding(String searchText, List<String> products, List<String> institutionTypes,
                                                List<String> statuses, OffsetDateTime createdFromDate, OffsetDateTime createdToDate,
                                                Long page, Long pageSize, List<String> orderBy, boolean includeTest) {
    page = page != null && page > 0L && page < Long.MAX_VALUE ? page : 0L;
    pageSize = pageSize != null && pageSize > 0L && pageSize < Long.MAX_VALUE ? pageSize : 15L;
    orderBy = orderBy != null && !orderBy.isEmpty() ? orderBy : List.of("description_ASC");
    final String filter = buildOnboardingFilter(products, institutionTypes, statuses, createdFromDate, createdToDate, includeTest);
    final String orderByString = buildOrderBy(orderBy);
    final OnboardingIndexSearch onboardingIndexSearch = searchServiceConnector.searchOnboarding(searchText, filter,
        pageSize, page * pageSize, orderByString);
    onboardingIndexSearch.setPage(page);
    onboardingIndexSearch.setPageSize(pageSize);
    onboardingIndexSearch.setTotalPages((onboardingIndexSearch.getTotalElements() + pageSize - 1) / pageSize);
    return onboardingIndexSearch;
  }

  private String buildOnboardingFilter(List<String> products, List<String> institutionTypes, List<String> statuses,
                                       OffsetDateTime createdFromDate, OffsetDateTime createdToDate, boolean includeTest) {
    final StringBuilder filter = new StringBuilder();

    if (products != null && !products.isEmpty()) {
      if (!filter.isEmpty()) {
        filter.append(AND);
      }
      filter.append("search.in(productId, '").append(String.join(",", products)).append("')");
    }

    if (institutionTypes != null && !institutionTypes.isEmpty()) {
      if (!filter.isEmpty()) {
        filter.append(AND);
      }
      filter.append("search.in(institutionType, '").append(String.join(",", institutionTypes)).append("')");
    }

    if (statuses != null && !statuses.isEmpty()) {
      if (!filter.isEmpty()) {
        filter.append(AND);
      }
      filter.append("search.in(status, '").append(String.join(",", statuses)).append("')");
    }

    if (createdFromDate != null) {
      if (!filter.isEmpty()) {
        filter.append(AND);
      }
      filter.append("createdAt ge ").append(createdFromDate.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    }

    if (createdToDate != null) {
      if (!filter.isEmpty()) {
        filter.append(AND);
      }
      filter.append("createdAt le ").append(createdToDate.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    }

    if(!includeTest) {
      if (!filter.isEmpty()) {
        filter.append(AND);
      }
      filter.append("isTest eq ").append(false);
    }

    return filter.toString();
  }

  @Override
  public IpaInstitutionSearchResult searchIpaInstitutions(String searchText, String category, Integer page, Integer pageSize) {
    page = page != null && page >= 0 ? page : 0;
    pageSize = pageSize != null && pageSize > 0 ? pageSize : 50;

    String search = searchText != null && !searchText.isBlank() ? searchText : "*";

    String filter = null;
    if (category != null && !category.isBlank()) {
      filter = "category eq '" + category + "'";
    }

    return ipaSearchServiceConnector.search(search, filter, pageSize, page * pageSize);
  }

  private String buildOrderBy(List<String> orderBy) {
    if (orderBy == null || orderBy.isEmpty()) {
      return "description asc";
    }

    return orderBy.stream()
            .map(param -> {
              String[] parts = param.split("_");

              if (parts.length != 2) {
                throw new IllegalArgumentException("OrderBy format not valid: " + param);
              }

              String field = parts[0];
              String direction = parts[1].toLowerCase();

              if (!direction.equals("asc") && !direction.equals("desc")) {
                throw new IllegalArgumentException("Order must be asc or desc: " + param);
              }

              return field + " " + direction;
            })
            .collect(Collectors.joining(","));
  }

}

