package it.pagopa.selfcare.party.registry_proxy.connector.model;

import lombok.Data;

import java.util.List;

@Data
public class OnboardingIndexSearch {

    private List<OnboardingIndex> onboardings;
    private long pageSize = 0;
    private long page = 0;
    private long totalElements = 0;
    private long totalPages = 0;

}
