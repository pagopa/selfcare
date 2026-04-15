package it.pagopa.selfcare.party.registry_proxy.web.model;

import lombok.Data;

import java.util.List;

@Data
public class OnboardingIndexSearchResource {

    private List<OnboardingIndexResource> onboardings;
    private long pageSize = 0;
    private long page = 0;
    private long totalElements = 0;
    private long totalPages = 0;

}
