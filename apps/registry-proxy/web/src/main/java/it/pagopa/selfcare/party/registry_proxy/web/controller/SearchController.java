package it.pagopa.selfcare.party.registry_proxy.web.controller;

import io.swagger.annotations.Api;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.pagopa.selfcare.party.registry_proxy.connector.model.OnboardingIndexSearch;
import it.pagopa.selfcare.party.registry_proxy.connector.model.SearchServiceInstitution;
import it.pagopa.selfcare.party.registry_proxy.core.SearchService;
import it.pagopa.selfcare.party.registry_proxy.web.model.OnboardingIndexSearchResource;
import it.pagopa.selfcare.party.registry_proxy.web.model.mapper.OnboardingMapper;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping(value = "/search")
@Api(tags = "search")
public class SearchController {

  private final SearchService searchService;
  private final OnboardingMapper onboardingMapper;

  public SearchController(SearchService searchService, OnboardingMapper onboardingMapper) {
    this.searchService = searchService;
    this.onboardingMapper = onboardingMapper;
  }

  @Tag(name = "external-v2")
  @Tag(name = "support")
  @Tag(name = "institution")
  @GetMapping("/institutions")
  @PreAuthorize("hasPermission(new it.pagopa.selfcare.party.registry_proxy.web.security.FilterAuthorityDomain('PAGOPA'), 'Selc:SearchInstitutions')")
  @Operation(summary = "${swagger.api.search.institutions.summary}",
    description = "${swagger.api.search.institutions.notes}",
    operationId = "retrieveInstitutionOnSearchEngine"
   )
  public ResponseEntity<List<SearchServiceInstitution>> searchInstitutions(
    @Parameter(description = "${swagger.model.*.products}")
    @RequestParam(required = false) List<String> products,
    @Parameter(description = "${swagger.model.*.institution.types}")
    @RequestParam(required = false) List<String> institutionTypes,
    @Parameter(description = "${swagger.model.institution.taxCode}")
    @RequestParam(required = false) String taxCode,
    @Parameter(description = "${swagger.model.*.searchText}")
    @RequestParam(defaultValue = "*") String searchText,
    @Parameter(description = "${swagger.model.*.limit}")
    @RequestParam(defaultValue = "50") int top,
    @Parameter(description = "${swagger.model.*.page}")
    @RequestParam(defaultValue = "0") int skip) {

    try {
      List<SearchServiceInstitution> institutions = searchService.searchInstitution(searchText, products, institutionTypes, taxCode, top, skip, null, null);

      return ResponseEntity.ok(institutions);

    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of());
    }
  }

  @Tag(name = "external-v2")
  @Tag(name = "support")
  @Tag(name = "institution")
  @GetMapping("/onboardings")
  @PreAuthorize("hasPermission(new it.pagopa.selfcare.party.registry_proxy.web.security.FilterAuthorityDomain('PAGOPA'), 'Selc:SearchInstitutions')")
  @Operation(summary = "${swagger.api.search.onboardings.summary}", description = "${swagger.api.search.onboardings.notes}", operationId = "retrieveOnboardingOnSearchEngine")
  public OnboardingIndexSearchResource searchOnboardings(@RequestParam(required = false) String searchText,
                                                         @RequestParam(required = false) List<String> products,
                                                         @RequestParam(required = false) List<String> institutionTypes,
                                                         @RequestParam(required = false) List<String> statuses,
                                                         @RequestParam(defaultValue = "0") @PositiveOrZero Long page,
                                                         @RequestParam(defaultValue = "15") @Positive Long pageSize,
                                                         @RequestParam(defaultValue = "description asc") String orderBy) {
    final OnboardingIndexSearch onboardingIndexSearch = searchService.searchOnboarding(searchText, products,
            institutionTypes, statuses, page, pageSize, orderBy);
    return onboardingMapper.toOnboardingIndexSearchResource(onboardingIndexSearch);
  }

}
