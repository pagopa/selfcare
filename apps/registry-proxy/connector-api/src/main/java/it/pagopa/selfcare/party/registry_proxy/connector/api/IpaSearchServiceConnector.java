package it.pagopa.selfcare.party.registry_proxy.connector.api;

import it.pagopa.selfcare.party.registry_proxy.connector.model.Institution;
import it.pagopa.selfcare.party.registry_proxy.connector.model.SearchServiceStatus;

import java.util.List;
import java.util.Map;

public interface IpaSearchServiceConnector {

    /**
     * Indexes a batch of IPA institutions in Azure AI Search
     * using the mergeOrUpload action (insert or update).
     *
     * @param institutions batch of IPA institutions to index (max 1000 per call)
     * @return indexing status from Azure AI Search
     */
    SearchServiceStatus indexInstitutions(List<Institution> institutions);

    /**
     * Fetches the full map of id → dataAggiornamento for all documents
     * currently stored in the IPA institution AI Search index.
     * Used by the scheduler to detect which records need updating.
     *
     * @return map keyed by document id, value is the stored dataAggiornamento string (may be null)
     */
    Map<String, String> fetchAllInstitutionDataAggiornamento();

}
