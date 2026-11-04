package it.pagopa.selfcare.onboarding.dto;

import java.util.List;

public record ContractUpdateCampaignRequest(String institutionId, List<String> productIds) {
}
