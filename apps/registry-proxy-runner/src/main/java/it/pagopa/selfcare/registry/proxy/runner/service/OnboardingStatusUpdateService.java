package it.pagopa.selfcare.registry.proxy.runner.service;

import it.pagopa.selfcare.registry.proxy.runner.client.AzureSearchRestClient;
import it.pagopa.selfcare.registry.proxy.runner.model.OnboardingSearchResponse;
import it.pagopa.selfcare.registry.proxy.runner.model.SearchServiceIndexRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Slf4j
@ApplicationScoped
public class OnboardingStatusUpdateService {

    @Inject
    @RestClient
    AzureSearchRestClient azureSearchRestClient;

    @ConfigProperty(name = "azure-ai-search.onboarding.index-name")
    String indexName;

    @ConfigProperty(name = "azure-ai-search.api-version")
    String apiVersion;

    private static final int PAGE_SIZE = 1000;

    public void updateExpiredOnboardings() {

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        String filter =
                "expiringDate le " + now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                        + " and status ne 'EXPIRED'";

        int totalUpdated = 0;
        boolean hasMoreResults;

        int skip = 0;

        do {

            OnboardingSearchResponse response =
                    azureSearchRestClient.searchOnboarding(
                            indexName,
                            apiVersion,
                            "*",
                            "onboardingId,status,expiringDate",
                            true,
                            PAGE_SIZE,
                            skip,
                            filter
                    );

            if (response == null || response.getValue() == null || response.getValue().isEmpty()) {
                hasMoreResults = false;
                continue;
            }

            List<Map<String, Object>> batchUpdates = response.getValue().stream()
                    .map(o -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("@search.action", "merge");
                        map.put("onboardingId", o.getOnboardingId());
                        map.put("status", "EXPIRED");
                        return map;
                    })
                    .toList();

            SearchServiceIndexRequest request = new SearchServiceIndexRequest();
            request.setValue(batchUpdates);

            azureSearchRestClient.index(indexName, apiVersion, request);

            totalUpdated += batchUpdates.size();

            skip += PAGE_SIZE;

            hasMoreResults = response.getValue().size() == PAGE_SIZE;

        } while (hasMoreResults);

        log.info("Onboarding expiration completed. Total updated: {}", totalUpdated);
    }
}