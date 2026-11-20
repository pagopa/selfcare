package it.pagopa.selfcare.onboarding.service;

import it.pagopa.selfcare.onboarding.dto.ContractUpdateCampaignRequest;
import it.pagopa.selfcare.onboarding.dto.ContractUpdateCampaignResponse;

public interface ContractUpdateCampaignService {

    /**
     * Sends the contract update campaign PEC to the institution and flags its adhesions as requiring a contract update.
     */
    ContractUpdateCampaignResponse send(ContractUpdateCampaignRequest request);
}
